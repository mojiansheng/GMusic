package dev.geco.gmusic.service;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.model.NotePart;
import dev.geco.gmusic.model.PlayListMode;
import dev.geco.gmusic.model.PlayMode;
import dev.geco.gmusic.model.PlaySettings;
import dev.geco.gmusic.model.PlayState;
import dev.geco.gmusic.model.Song;
import dev.geco.gmusic.model.gui.MusicGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;

public class JukeBoxService {

	private final GMusicMain gMusicMain;
	private final NamespacedKey jukeBoxKey;
	private final HashMap<Block, UUID> jukeBoxBlocks = new HashMap<>();
	private final HashMap<UUID, Block> jukeBoxes = new HashMap<>();
	private final Random random = new Random();

	public JukeBoxService(GMusicMain gMusicMain) {
		this.gMusicMain = gMusicMain;
		jukeBoxKey = new NamespacedKey(gMusicMain, GMusicMain.NAME + "_juke_box");
	}

	public void createDataTables() {
		try {
			gMusicMain.getDataService().execute("CREATE TABLE IF NOT EXISTS gmusic_juke_box (uuid TEXT, world TEXT, x INTEGER, y INTEGER, z INTEGER);");
		} catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not create juke box database tables!", e); }
	}

	public @NotNull NamespacedKey getJukeBoxKey() { return jukeBoxKey; }

	public @NotNull ItemStack createJukeBoxItem() {
		ItemStack itemStack = new ItemStack(Material.JUKEBOX);
		itemStack.setAmount(1);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(gMusicMain.getMessageService().getMessage("Items.jukebox-title"));
		itemMeta.setLore(List.of(gMusicMain.getMessageService().getMessage("Items.jukebox-description")));
		itemMeta.getPersistentDataContainer().set(jukeBoxKey, PersistentDataType.BOOLEAN, true);
		itemMeta.addItemFlags(ItemFlag.values());
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public @Nullable UUID getJukeBoxId(Block block) { return jukeBoxBlocks.get(block); }

	public @Nullable Block getJukeBoxBlock(@NotNull UUID uuid) { return jukeBoxes.get(uuid); }

	public void addTemporaryJukeBoxBlock(@NotNull UUID uuid, @NotNull Block block) { jukeBoxes.put(uuid, block); }

	public void removeTemporaryJukeBoxBlock(@NotNull UUID uuid) { jukeBoxes.remove(uuid); }

	public void loadJukeboxes(@NotNull World world) {
		jukeBoxBlocks.clear();
		jukeBoxes.clear();
		gMusicMain.getTaskService().runDelayed(() -> {
			try {
				try(ResultSet jukeBoxData = gMusicMain.getDataService().executeAndGet("SELECT * FROM gmusic_juke_box")) {
					while(jukeBoxData.next()) {
						String worldName = jukeBoxData.getString("world");
						World jukeBoxWorld = Bukkit.getWorld(worldName);
						if(jukeBoxWorld == null || (world != null && world.equals(jukeBoxWorld))) continue;

						Location location = new Location(jukeBoxWorld, jukeBoxData.getInt("x"), jukeBoxData.getInt("y"), jukeBoxData.getInt("z"));

						UUID uuid = UUID.fromString(jukeBoxData.getString("uuid"));

						Block block = location.getBlock();
						if(block.getType() != Material.JUKEBOX) {
							gMusicMain.getDataService().execute("DELETE FROM gmusic_juke_box WHERE uuid = ?", uuid.toString());
							continue;
						}

						PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);

						jukeBoxBlocks.put(block, uuid);
						jukeBoxes.put(uuid, block);
						if(playSettings.getPlayListMode() == PlayListMode.RADIO) gMusicMain.getRadioService().addRadioJukeBox(uuid, block);
						else if(playSettings.isPlayOnJoin()) {
							if(gMusicMain.getPlayService().hasPlayingSong(uuid)) resumeBoxSong(uuid);
							else {
								Song song = playSettings.getCurrentSong() != null ? gMusicMain.getSongService().getSongById(playSettings.getCurrentSong()) : null;
								playBoxSong(uuid, song != null ? song : gMusicMain.getPlayService().getRandomSong(uuid));
							}
						}

						new MusicGUI(uuid, MusicGUI.MenuType.JUKEBOX);
					}
				}
			} catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not load jukeboxes", e); }
		}, 0);
	}

	public void setJukebox(@NotNull Block block) {
		try {
			UUID uuid = UUID.randomUUID();
			gMusicMain.getDataService().execute("INSERT INTO gmusic_juke_box (uuid, world, x, y, z) VALUES (?, ?, ?, ?, ?)",
					uuid.toString(),
					block.getWorld().getName(),
					block.getX(),
					block.getY(),
					block.getZ()
			);
			PlaySettings playSettings = gMusicMain.getPlaySettingsService().generateDefaultPlaySettings(uuid);
			playSettings.setRange(gMusicMain.getConfigService().JUKEBOX_RANGE);
			if(playSettings.getPlayListMode() == PlayListMode.RADIO) gMusicMain.getRadioService().addRadioJukeBox(uuid, block);
			jukeBoxBlocks.put(block, uuid);
			jukeBoxes.put(uuid, block);
			new MusicGUI(uuid, MusicGUI.MenuType.JUKEBOX);
		} catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not set jukebox", e); }
	}

	public void removeJukebox(@NotNull Block block) {
		UUID uuid = jukeBoxBlocks.get(block);
		if(uuid == null) return;
		stopBoxSong(uuid);
		gMusicMain.getPlaySettingsService().savePlaySettings(uuid, null);
		MusicGUI.getMusicGUI(uuid).close(true);
		gMusicMain.getRadioService().removeRadioJukeBox(uuid);
		jukeBoxBlocks.remove(block);
		jukeBoxes.remove(uuid);
		try {
			gMusicMain.getDataService().execute("DELETE FROM gmusic_juke_box WHERE uuid = ?", uuid.toString());
		} catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not remove jukebox", e); }
	}

	public HashMap<Player, Double> getPlayersInRange(@NotNull Location location, long range) {
		HashMap<Player, Double> playerRangeMap = new HashMap<>();
		try {
			for(Player player : location.getWorld().getPlayers()) {
				double distance = location.distance(player.getLocation());
				PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(player.getUniqueId());
				if(playSettings != null && distance <= range && !playSettings.isToggleMode()) playerRangeMap.put(player, distance);
			}
		} catch(Throwable ignored) { }
		return playerRangeMap;
	}

	public void playBoxSong(@NotNull UUID uuid, @Nullable Song song) { playBoxSong(uuid, song, 0); }

	private void playBoxSong(@NotNull UUID uuid, @Nullable Song song, long delay) {
		if(song == null) return;

		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);

		PlayState playState = gMusicMain.getPlayService().getPlayState(uuid);
		if(playState != null) playState.getTimer().cancel();

		Timer timer = new Timer();
		playState = new PlayState(song, timer, playSettings.isReverseMode() ? song.getLength() + delay : -delay);
		gMusicMain.getPlayService().setPlayState(uuid, playState);

		playSettings.setCurrentSong(song.getId());

		if(gMusicMain.getConfigService().A_SHOW_MESSAGES) {
			Block block = jukeBoxes.get(uuid);
			if(block != null) {
				for(Player player : getPlayersInRange(block.getLocation().add(0.5, 0, 0.5), playSettings.getRange()).keySet()) {
					gMusicMain.getMessageService().sendActionBarMessage(
						player,
						"Messages.actionbar-play",
						"%Song%", song.getId(),
						"%SongTitle%", song.getTitle(),
						"%Author%", song.getAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(),
						"%OriginalAuthor%", song.getOriginalAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-original-author") : song.getOriginalAuthor()
					);
				}
			}
		}

		playBoxTimer(uuid, song, timer);
	}

	private void playBoxTimer(@NotNull UUID uuid, @NotNull Song song, @NotNull Timer timer) {
		PlayState playState = gMusicMain.getPlayService().getPlayState(uuid);
		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);

		Block block = jukeBoxes.get(uuid);
		if(block == null) return;
		Location boxLocation = block.getLocation().add(0.5, 0, 0.5);

		final long[] ticker = {0};

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				ticker[0]++;

				long position = playState.getTickPosition();

				List<NotePart> noteParts = song.getContent().get(position);

				HashMap<Player, Double> playersInRange = getPlayersInRange(boxLocation, playSettings.getRange());

				if(noteParts != null && playSettings.getVolume() > 0 && !playersInRange.isEmpty()) {
					if(playSettings.isShowingParticles()) {
						Location particleLocation = boxLocation.clone().add(random.nextDouble() - 0.5, 1.25, random.nextDouble() - 0.5);
						for(Player player : playersInRange.keySet()) player.spawnParticle(Particle.NOTE, particleLocation, 0, random.nextDouble(), random.nextDouble(), random.nextDouble(), 1);
					}

					for(NotePart notePart : noteParts) {
						for(Player player : playersInRange.keySet()) {
							if(notePart.getSound() != null) {
								float volume = (float) ((playersInRange.get(player) - playSettings.getRange()) * playSettings.getFixedVolume() / (double) -playSettings.getRange()) * notePart.getVolume();

								Location location = notePart.getDistance() == 0 ? player.getEyeLocation() : gMusicMain.getSteroNoteUtil().convertToStero(player.getEyeLocation(), notePart.getDistance());

								if(!gMusicMain.getConfigService().ENVIRONMENT_EFFECTS) player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume, notePart.getPitch());
								else {
									if(gMusicMain.getEnvironmentUtil().isPlayerSwimming(player)) player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume > 0.4f ? volume - 0.3f : volume, notePart.getPitch() - 0.15f);
									else player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume, notePart.getPitch());
								}
							} else if(notePart.getStopSound() != null) player.stopSound(notePart.getStopSound(), song.getSoundCategory());
						}
					}
				}

				if(position == (playSettings.isReverseMode() ? 0 : song.getLength())) {
					if(playSettings.getPlayMode() == PlayMode.LOOP && playSettings.getPlayListMode() != PlayListMode.RADIO) {
						position = playSettings.isReverseMode() ? song.getLength() + gMusicMain.getConfigService().PS_TIME_UNTIL_REPEAT : -gMusicMain.getConfigService().PS_TIME_UNTIL_REPEAT;
						playState.setTickPosition(position);
					} else {
						timer.cancel();

						if(playSettings.getPlayMode() == PlayMode.SHUFFLE && playSettings.getPlayListMode() != PlayListMode.RADIO) playBoxSong(uuid, gMusicMain.getPlayService().getShuffleSong(uuid, song), gMusicMain.getConfigService().PS_TIME_UNTIL_SHUFFLE);
						else {
							gMusicMain.getPlayService().removePlayState(uuid);
							MusicGUI m = MusicGUI.getMusicGUI(uuid);
							if(m != null) m.setPauseResumeBar();
						}
					}
				} else {
					playState.setTickPosition(playSettings.isReverseMode() ? position - 1 : position + 1);
					if(gMusicMain.getConfigService().A_SHOW_WHILE_PLAYING && ticker[0] % 2000 == 0) {
						for(Player player : playersInRange.keySet()) {
							gMusicMain.getMessageService().sendActionBarMessage(
								player,
								"Messages.actionbar-play",
								"%Song%", song.getId(),
								"%SongTitle%", song.getTitle(),
								"%Author%", song.getAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(),
								"%OriginalAuthor%", song.getOriginalAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-original-author") : song.getOriginalAuthor()
							);
						}
					}
				}
			}
		}, 0, 1);
	}

	public @Nullable Song getNextSong(@NotNull UUID uuid) {
		PlayState playState = gMusicMain.getPlayService().getPlayState(uuid);
		return playState != null ? gMusicMain.getPlayService().getShuffleSong(uuid, playState.getSong()) : gMusicMain.getPlayService().getRandomSong(uuid);
	}

	public void stopBoxSong(@NotNull UUID uuid) {
		PlayState playState = gMusicMain.getPlayService().getPlayState(uuid);
		if(playState == null) return;

		playState.getTimer().cancel();

		gMusicMain.getPlayService().removePlayState(uuid);

		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);
		playSettings.setCurrentSong(null);

		if(gMusicMain.getConfigService().A_SHOW_MESSAGES) {
			Block block = jukeBoxes.get(uuid);
			if(block != null) {
				for(Player player : getPlayersInRange(block.getLocation().add(0.5, 0, 0.5), playSettings.getRange()).keySet()) {
					gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-stop");
				}
			}
		}
	}

	public void pauseBoxSong(@NotNull UUID uuid) {
		PlayState playState = gMusicMain.getPlayService().getPlayState(uuid);
		if(playState == null) return;

		playState.getTimer().cancel();
		playState.setPaused(true);

		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);
		if(gMusicMain.getConfigService().A_SHOW_MESSAGES && playSettings != null) {
			Block block = jukeBoxes.get(uuid);
			if(block != null) {
				for(Player player : getPlayersInRange(block.getLocation().add(0.5, 0, 0.5), playSettings.getRange()).keySet()) {
					gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-pause");
				}
			}
		}
	}

	public void resumeBoxSong(@NotNull UUID uuid) {
		PlayState playState = gMusicMain.getPlayService().getPlayState(uuid);
		if(playState == null) return;

		playState.setTimer(new Timer());
		playState.setPaused(false);

		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);
		if(gMusicMain.getConfigService().A_SHOW_MESSAGES && playSettings != null) {
			Block block = jukeBoxes.get(uuid);
			if(block != null) {
				for(Player player : getPlayersInRange(block.getLocation().add(0.5, 0, 0.5), playSettings.getRange()).keySet()) {
					gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-resume");
				}
			}
		}

		playBoxTimer(uuid, playState.getSong(), playState.getTimer());
	}

}