package dev.geco.gmusic.event;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.object.GPlayMode;
import dev.geco.gmusic.object.GPlaySettings;
import dev.geco.gmusic.object.GSong;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class DiscEventHandler implements Listener {

	private final GMusicMain gMusicMain;

	public DiscEventHandler(GMusicMain gMusicMain) {
		this.gMusicMain = gMusicMain;
	}

	@EventHandler (priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if(event.getAction() != Action.RIGHT_CLICK_BLOCK || player.isSneaking()) return;

		Block block = event.getClickedBlock();
		if(block == null || block.getType() != Material.JUKEBOX) return;

		if(gMusicMain.getJukeBoxService().getJukeBoxId(block) != null) return;

		Jukebox jukebox = (Jukebox) block.getState();
		ItemStack record = jukebox.getRecord();
		if(record.getType() == Material.AIR) {
			ItemStack item = event.getItem();
			if(item == null) return;

			if(!item.getItemMeta().getPersistentDataContainer().has(gMusicMain.getDiscService().getDiscKey())) return;

			String songId = item.getItemMeta().getPersistentDataContainer().get(gMusicMain.getDiscService().getDiscKey(), PersistentDataType.STRING);
			if(songId == null) return;

			event.setCancelled(true);

			if(!gMusicMain.getPermissionService().hasPermission(player, "Disc")) return;

			if(!gMusicMain.getEnvironmentUtil().isEntityInAllowedWorld(player)) return;

			GSong song = gMusicMain.getSongService().getSongById(songId);
			if(song == null) return;

			if(player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);

			UUID uuid = UUID.randomUUID();
			GPlaySettings playSettings = gMusicMain.getPlaySettingsService().generateDefaultPlaySettings(uuid);
			playSettings.setRange(gMusicMain.getConfigService().JUKEBOX_RANGE);
			playSettings.setPlayMode(GPlayMode.DEFAULT);
			playSettings.setShowParticles(true);

			gMusicMain.getJukeBoxService().addTemporaryJukeBoxBlock(uuid, block);
			gMusicMain.getJukeBoxService().playBoxSong(uuid, song);

			ItemStack placeholder = new ItemStack(Material.STICK);
			ItemMeta itemMeta = placeholder.getItemMeta();
			itemMeta.setDisplayName(uuid.toString());
			itemMeta.getPersistentDataContainer().set(gMusicMain.getDiscService().getDiscKey(), PersistentDataType.STRING, songId);
			placeholder.setItemMeta(itemMeta);

			jukebox.setRecord(placeholder);
			jukebox.update(false, false);
		} else {
			handleRecordEject(jukebox, record);
		}
	}

	@EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void blockBreakEvent(BlockBreakEvent event) {
		handleBlockBreak(event.getBlock());
	}

	@EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		for(Block block : event.blockList()) handleBlockBreak(block);
	}

	@EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		for(Block block : event.blockList()) handleBlockBreak(block);
	}

	private void handleBlockBreak(Block block) {
		if(block.getType() != Material.JUKEBOX) return;

		Jukebox jukebox = (Jukebox) block.getState();
		ItemStack record = jukebox.getRecord();
		if(record.getType() == Material.AIR) return;

		handleRecordEject(jukebox, record);
	}

	private void handleRecordEject(Jukebox jukebox, ItemStack record) {
		if(!record.getItemMeta().getPersistentDataContainer().has(gMusicMain.getDiscService().getDiscKey())) return;

		String songId = record.getItemMeta().getPersistentDataContainer().get(gMusicMain.getDiscService().getDiscKey(), PersistentDataType.STRING);
		if(songId == null) return;

		try {
			UUID uuid = UUID.fromString(record.getItemMeta().getDisplayName());
			gMusicMain.getJukeBoxService().removeTemporaryJukeBoxBlock(uuid);
			gMusicMain.getJukeBoxService().stopBoxSong(uuid);
			gMusicMain.getPlaySettingsService().removePlaySettingsCache(uuid);
		} catch(IllegalArgumentException ignored) {}

		jukebox.setRecord(null);
		jukebox.update(false, false);

		GSong song = gMusicMain.getSongService().getSongById(songId);
		if(song == null) return;

		Location dropLocation = jukebox.getLocation().add(0.5, 1.01, 0.5);
		jukebox.getWorld().dropItem(dropLocation, gMusicMain.getDiscService().createDiscItem(song));
	}

}