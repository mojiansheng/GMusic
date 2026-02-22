package dev.geco.gmusic.service;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.object.GNotePart;
import dev.geco.gmusic.object.GPlayListMode;
import dev.geco.gmusic.object.GPlaySettings;
import dev.geco.gmusic.object.GPlayState;
import dev.geco.gmusic.object.GSong;
import dev.geco.gmusic.object.gui.GMusicGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class RadioService {

	private final GMusicMain gMusicMain;
	private final Random random = new Random();
	private UUID radioUUID;
	private final Set<Player> radioPlayers = new HashSet<>();
	private final HashMap<UUID, Block> radioJukeBoxBlocks = new HashMap<>();

	public RadioService(GMusicMain gMusicMain) {
		this.gMusicMain = gMusicMain;
	}

	public void startRadio() {
		radioPlayers.clear();
		radioJukeBoxBlocks.clear();
		radioUUID = UUID.randomUUID();

		for(Player player : Bukkit.getOnlinePlayers()) {
			GPlaySettings playerPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(player.getUniqueId());
			if(playerPlaySettings.getPlayListMode() == GPlayListMode.RADIO) {
				radioPlayers.add(player);
			}
		}

		playSong(gMusicMain.getPlayService().getRandomSong(radioUUID), 0);

		new GMusicGUI(radioUUID, GMusicGUI.MenuType.RADIO);
	}

	public UUID getRadioUUID() { return radioUUID; }

	public void playSong(GSong song) { playSong(song, 0); }

	public void playSong(GSong song, long delay) {
		if(song == null) return;

		GPlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioUUID);

		GPlayState playState = gMusicMain.getPlayService().getPlayState(radioUUID);
		if(playState != null) playState.getTimer().cancel();

		Timer timer = new Timer();
		playState = new GPlayState(song, timer, playSettings.isReverseMode() ? song.getLength() + delay : -delay);
		gMusicMain.getPlayService().setPlayState(radioUUID, playState);

		playSettings.setCurrentSong(song.getId());

		if(gMusicMain.getConfigService().A_SHOW_MESSAGES) {
			Set<Player> players = new HashSet<>(radioPlayers);
			for(Map.Entry<UUID, Block> radioJukeBox : radioJukeBoxBlocks.entrySet()) {
				GPlaySettings jukeBoxPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioJukeBox.getKey());
				HashMap<Player, Double> a = gMusicMain.getJukeBoxService().getPlayersInRange(radioJukeBox.getValue().getLocation().add(0.5, 0, 0.5), jukeBoxPlaySettings.getRange());
				players.addAll(a.keySet());
			}
			for(Player player : players) {
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

		playTimer(song, timer);
	}

	private void playTimer(GSong song, Timer timer) {
		GPlayState playState = gMusicMain.getPlayService().getPlayState(radioUUID);
		GPlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioUUID);

		final long[] ticker = {0};

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				ticker[0]++;

				long position = playState.getTickPosition();

				List<GNotePart> noteParts = song.getContent().get(position);

				List<Player> players = new ArrayList<>(radioPlayers);

				if(noteParts != null && playSettings.getVolume() > 0 && (!players.isEmpty() || !radioJukeBoxBlocks.isEmpty())) {
					for(Map.Entry<UUID, Block> radioJukeBox : radioJukeBoxBlocks.entrySet()) {
						GPlaySettings jukeBoxPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioJukeBox.getKey());
						if(jukeBoxPlaySettings.isShowingParticles()) {
							Location boxLocation = radioJukeBox.getValue().getLocation().add(0.5, 0, 0.5);
							HashMap<Player, Double> playersInRange = gMusicMain.getJukeBoxService().getPlayersInRange(boxLocation, jukeBoxPlaySettings.getRange());
							Location particleLocation = boxLocation.clone().add(random.nextDouble() - 0.5, 1.25, random.nextDouble() - 0.5);
							for(Player player : playersInRange.keySet()) player.spawnParticle(Particle.NOTE, particleLocation, 0, random.nextDouble(), random.nextDouble(), random.nextDouble(), 1);
						}
					}

					for(Player player : players) {
						if(player == null) continue;
						GPlaySettings playerPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(player.getUniqueId());
						if(!playerPlaySettings.isShowingParticles()) continue;
						player.spawnParticle(Particle.NOTE, player.getEyeLocation().clone().add(random.nextDouble() - 0.5, 0.3, random.nextDouble() - 0.5), 0, random.nextDouble(), random.nextDouble(), random.nextDouble(), 1);
					}

					for(GNotePart notePart : noteParts) {
						for(Player player : players) {
							if (player == null) continue;
							if (notePart.getSound() != null) {
								GPlaySettings playerPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(player.getUniqueId());
								float volume = playerPlaySettings.getFixedVolume() * notePart.getVolume();

								Location location = notePart.getDistance() == 0 ? player.getEyeLocation() : gMusicMain.getSteroNoteUtil().convertToStero(player.getEyeLocation(), notePart.getDistance());

								if (!gMusicMain.getConfigService().ENVIRONMENT_EFFECTS)
									player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume, notePart.getPitch());
								else {
									if (gMusicMain.getEnvironmentUtil().isPlayerSwimming(player))
										player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume > 0.4f ? volume - 0.3f : volume, notePart.getPitch() - 0.15f);
									else
										player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume, notePart.getPitch());
								}
							} else if (notePart.getStopSound() != null)
								player.stopSound(notePart.getStopSound(), song.getSoundCategory());
						}

						for(Map.Entry<UUID, Block> radioJukeBox : radioJukeBoxBlocks.entrySet()) {
							GPlaySettings jukeBoxPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioJukeBox.getKey());
							Location boxLocation = radioJukeBox.getValue().getLocation().add(0.5, 0, 0.5);
							HashMap<Player, Double> playersInRange = gMusicMain.getJukeBoxService().getPlayersInRange(boxLocation, jukeBoxPlaySettings.getRange());
							for(Player player : playersInRange.keySet()) {
								if (notePart.getSound() != null) {
									float volume = (float) ((playersInRange.get(player) - jukeBoxPlaySettings.getRange()) * jukeBoxPlaySettings.getFixedVolume() / (double) -jukeBoxPlaySettings.getRange()) * notePart.getVolume();

									Location location = notePart.getDistance() == 0 ? player.getEyeLocation() : gMusicMain.getSteroNoteUtil().convertToStero(player.getEyeLocation(), notePart.getDistance());

									if (!gMusicMain.getConfigService().ENVIRONMENT_EFFECTS) player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume, notePart.getPitch());
									else {
										if (gMusicMain.getEnvironmentUtil().isPlayerSwimming(player)) player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume > 0.4f ? volume - 0.3f : volume, notePart.getPitch() - 0.15f);
										else player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume, notePart.getPitch());
									}
								} else if (notePart.getStopSound() != null) player.stopSound(notePart.getStopSound(), song.getSoundCategory());
							}
							players.addAll(playersInRange.keySet());
						}
					}
				}

				if(position == (playSettings.isReverseMode() ? 0 : song.getLength())) {
					timer.cancel();
					playSong(gMusicMain.getPlayService().getShuffleSong(radioUUID, song), gMusicMain.getConfigService().PS_TIME_UNTIL_SHUFFLE);
				} else {
					playState.setTickPosition(playSettings.isReverseMode() ? position - 1 : position + 1);
					if(gMusicMain.getConfigService().A_SHOW_WHILE_PLAYING && ticker[0] % 2000 == 0) {
						for(Player radioPlayer : players) {
							if(radioPlayer == null) continue;
							gMusicMain.getMessageService().sendActionBarMessage(
								radioPlayer,
								"Messages.actionbar-play",
								"%Song%", song.getId(),
								"%SongTitle%", song.getTitle(),
								"%Author%", song.getAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(),
								"%OAuthor%", song.getOriginalAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-oauthor") : song.getOriginalAuthor()
							);
						}
					}
				}
			}
		}, 0, 1);
	}

	public void stopRadio() {
		gMusicMain.getPlaySettingsService().removePlaySettingsCache(radioUUID);

		GPlayState playState = gMusicMain.getPlayService().getPlayState(radioUUID);
		if(playState == null) return;

		playState.getTimer().cancel();

		gMusicMain.getPlayService().removePlayState(radioUUID);
	}

	public GSong getNextSong() {
		GPlayState playState = gMusicMain.getPlayService().getPlayState(radioUUID);
		return playState != null ? gMusicMain.getPlayService().getShuffleSong(radioUUID, playState.getSong()) : gMusicMain.getPlayService().getRandomSong(radioUUID);
	}

	public void stopSong() {
		GPlayState playState = gMusicMain.getPlayService().getPlayState(radioUUID);
		if(playState == null) return;

		playState.getTimer().cancel();

		gMusicMain.getPlayService().removePlayState(radioUUID);

		GPlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioUUID);
		playSettings.setCurrentSong(null);

		if(gMusicMain.getConfigService().A_SHOW_MESSAGES) {
			Set<Player> players = new HashSet<>(radioPlayers);
			for(Map.Entry<UUID, Block> radioJukeBox : radioJukeBoxBlocks.entrySet()) {
				GPlaySettings jukeBoxPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioJukeBox.getKey());
				HashMap<Player, Double> a = gMusicMain.getJukeBoxService().getPlayersInRange(radioJukeBox.getValue().getLocation().add(0.5, 0, 0.5), jukeBoxPlaySettings.getRange());
				players.addAll(a.keySet());
			}
			for(Player player : players) {
				gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-pause");
			}
		}
	}

	public void pauseSong() {
		GPlayState playState = gMusicMain.getPlayService().getPlayState(radioUUID);
		if(playState == null) return;

		playState.getTimer().cancel();
		playState.setPaused(true);

		GPlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioUUID);
		if(gMusicMain.getConfigService().A_SHOW_MESSAGES && playSettings != null) {
			Set<Player> players = new HashSet<>(radioPlayers);
			for(Map.Entry<UUID, Block> radioJukeBox : radioJukeBoxBlocks.entrySet()) {
				GPlaySettings jukeBoxPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioJukeBox.getKey());
				HashMap<Player, Double> a = gMusicMain.getJukeBoxService().getPlayersInRange(radioJukeBox.getValue().getLocation().add(0.5, 0, 0.5), jukeBoxPlaySettings.getRange());
				players.addAll(a.keySet());
			}
			for(Player player : players) {
				gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-pause");
			}
		}
	}

	public void resumeSong() {
		GPlayState playState = gMusicMain.getPlayService().getPlayState(radioUUID);
		if(playState == null) return;

		playState.setTimer(new Timer());
		playState.setPaused(false);

		GPlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioUUID);
		if(gMusicMain.getConfigService().A_SHOW_MESSAGES && playSettings != null) {
			Set<Player> players = new HashSet<>(radioPlayers);
			for(Map.Entry<UUID, Block> radioJukeBox : radioJukeBoxBlocks.entrySet()) {
				GPlaySettings jukeBoxPlaySettings = gMusicMain.getPlaySettingsService().getPlaySettings(radioJukeBox.getKey());
				HashMap<Player, Double> a = gMusicMain.getJukeBoxService().getPlayersInRange(radioJukeBox.getValue().getLocation().add(0.5, 0, 0.5), jukeBoxPlaySettings.getRange());
				players.addAll(a.keySet());
			}
			for(Player player : players) {
				gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-resume");
			}
		}

		playTimer(playState.getSong(), playState.getTimer());
	}

	public void removeRadioPlayer(Player Player) { radioPlayers.remove(Player); }

	public void addRadioPlayer(Player Player) { radioPlayers.add(Player); }

	public void removeRadioJukeBox(UUID uuid) { radioJukeBoxBlocks.remove(uuid); }

	public void addRadioJukeBox(UUID uuid, Block block) { radioJukeBoxBlocks.put(uuid, block); }

}