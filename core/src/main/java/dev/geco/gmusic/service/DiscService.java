package dev.geco.gmusic.service;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.object.GSong;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class DiscService {

	private final GMusicMain gMusicMain;
	private final NamespacedKey discKey;

	public DiscService(GMusicMain gMusicMain) {
		this.gMusicMain = gMusicMain;
		discKey = new NamespacedKey(gMusicMain, GMusicMain.NAME + "_disc");
	}

	public NamespacedKey getDiscKey() { return discKey; }

	public ItemStack createDiscItem(GSong song) {
		ItemStack itemStack = new ItemStack(song.getDiscMaterial());
		itemStack.setAmount(1);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage(
			"Items.disc-title",
			"%Song%", song.getId(),
			"%SongTitle%", song.getTitle(),
			"%Author%", song.getAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(),
			"%OriginalAuthor%", song.getOriginalAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-original-author") : song.getOriginalAuthor()
		));
		itemMeta.setLore(List.of(gMusicMain.getMessageService().getMessage(
			"Items.disc-description",
			"%Song%", song.getId(),
			"%SongTitle%", song.getTitle(),
			"%Author%", song.getAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(),
			"%OriginalAuthor%", song.getOriginalAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-original-author") : song.getOriginalAuthor()
		)));
		itemMeta.getPersistentDataContainer().set(discKey, PersistentDataType.STRING, song.getId());
		itemMeta.addItemFlags(ItemFlag.values());
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

}