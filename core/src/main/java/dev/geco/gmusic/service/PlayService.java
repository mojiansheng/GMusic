package dev.geco.gmusic.service;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.model.PlayListMode;
import dev.geco.gmusic.model.PlayMode;
import dev.geco.gmusic.model.gui.MusicGUI;
import dev.geco.gmusic.model.NotePart;
import dev.geco.gmusic.model.PlaySettings;
import dev.geco.gmusic.model.Song;
import dev.geco.gmusic.model.PlayState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class PlayService {

	private final GMusicMain gMusicMain;
	private final Random random = new Random();
	private final HashMap<UUID, PlayState> playStates = new HashMap<>();

	public PlayService(GMusicMain gMusicMain) {
		this.gMusicMain = gMusicMain;
	}

	public void playSong(@NotNull Player player, @Nullable Song song) { playSong(player, song, 0); }

	private void playSong(@NotNull Player player, @Nullable Song song, long delay) {
		if(song == null) return;

		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(player.getUniqueId());
		if(playSettings.getPlayListMode() == PlayListMode.RADIO) return;

		PlayState playState = getPlayState(player.getUniqueId());
		if(playState != null) playState.getTimer().cancel();

		Timer timer = new Timer();
		playState = new PlayState(song, timer, playSettings.isReverseMode() ? song.getLength() + delay : -delay);
		setPlayState(player.getUniqueId(), playState);

		playSettings.setCurrentSong(song.getId());

		if(gMusicMain.getConfigService().A_SHOW_MESSAGES) {
			gMusicMain.getMessageService().sendActionBarMessage(
				player,
				"Messages.actionbar-play",
				"%Song%", song.getId(),
				"%SongTitle%", song.getTitle(),
				"%Author%", song.getAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(),
				"%OriginalAuthor%", song.getOriginalAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-original-author") : song.getOriginalAuthor()
			);
		}

		startSong(player, song, timer);
	}

	public @Nullable Song getRandomSong(@NotNull UUID uuid) {
		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);
		List<Song> songs = playSettings.getPlayListMode() == PlayListMode.FAVORITES ? playSettings.getFavorites() : gMusicMain.getSongService().getSongs();
		return !songs.isEmpty() ? songs.get(random.nextInt(songs.size())) : null;
	}

	public @Nullable Song getShuffleSong(@NotNull UUID uuid, @NotNull Song song) {
		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(uuid);
		List<Song> songs = playSettings.getPlayListMode() == PlayListMode.FAVORITES ? playSettings.getFavorites() : gMusicMain.getSongService().getSongs();
		return !songs.isEmpty() ? songs.indexOf(song) + 1 == songs.size() ? songs.get(0) : songs.get(songs.indexOf(song) + 1) : null;
	}

	private void startSong(@NotNull Player player, @NotNull Song song, @NotNull Timer timer) {
		UUID uuid = player.getUniqueId();
		PlayState playState = getPlayState(uuid);
		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(player.getUniqueId());

		final long[] ticker = {0};

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				ticker[0]++;

				long position = playState.getTickPosition();

				List<NotePart> noteParts = song.getContent().get(position);

				if(noteParts != null && playSettings.getVolume() > 0) {
					if(playSettings.isShowingParticles()) player.spawnParticle(Particle.NOTE, player.getEyeLocation().add(random.nextDouble() - 0.5, 0.3, random.nextDouble() - 0.5), 0, random.nextDouble(), random.nextDouble(), random.nextDouble(), 1);

					for(NotePart notePart : noteParts) {
						if(notePart.getSound() != null) {
							float volume = playSettings.getFixedVolume() * notePart.getVolume();

							Location location = notePart.getDistance() == 0 ? player.getEyeLocation() : gMusicMain.getSteroNoteUtil().convertToStero(player.getEyeLocation(), notePart.getDistance());

							if(!gMusicMain.getConfigService().ENVIRONMENT_EFFECTS) player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume, notePart.getPitch());
							else {
								if(gMusicMain.getEnvironmentUtil().isPlayerSwimming(player)) player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume > 0.4f ? volume - 0.3f : volume, notePart.getPitch() - 0.15f);
								else player.playSound(location, notePart.getSound(), song.getSoundCategory(), volume, notePart.getPitch());
							}
						} else if(notePart.getStopSound() != null) player.stopSound(notePart.getStopSound(), song.getSoundCategory());
					}
				}

				if(position == (playSettings.isReverseMode() ? 0 : song.getLength())) {
					if(playSettings.getPlayMode() == PlayMode.LOOP) {
						position = playSettings.isReverseMode() ? song.getLength() + gMusicMain.getConfigService().PS_TIME_UNTIL_REPEAT : -gMusicMain.getConfigService().PS_TIME_UNTIL_REPEAT;
						playState.setTickPosition(position);
					} else {
						timer.cancel();

						if(playSettings.getPlayMode() == PlayMode.SHUFFLE) playSong(player, getShuffleSong(uuid, song), gMusicMain.getConfigService().PS_TIME_UNTIL_SHUFFLE);
						else {
							playStates.remove(uuid);
							MusicGUI musicGUI = MusicGUI.getMusicGUI(uuid);
							if(musicGUI != null) musicGUI.setPauseResumeBar();
						}
					}
					return;
				}

				playState.setTickPosition(playSettings.isReverseMode() ? position - 1 : position + 1);

				if(gMusicMain.getConfigService().A_SHOW_WHILE_PLAYING && ticker[0] % 2000 == 0) {
					gMusicMain.getMessageService().sendActionBarMessage(
						player,
						"Messages.actionbar-playing",
						"%Song%", song.getId(),
						"%SongTitle%", song.getTitle(),
						"%Author%", song.getAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-author") : song.getAuthor(),
						"%OriginalAuthor%", song.getOriginalAuthor().isEmpty() ? gMusicMain.getMessageService().getMessage("MusicGUI.disc-empty-original-author") : song.getOriginalAuthor()
					);
				}
			}
		}, 0, 1);
	}

	public @Nullable PlayState getPlayState(@NotNull UUID uuid) { return playStates.get(uuid); }

	public void removePlayState(@NotNull UUID uuid) { playStates.remove(uuid); }

	public void setPlayState(@NotNull UUID uuid, @NotNull PlayState playState) { playStates.put(uuid, playState); }

	public boolean hasPlayingSong(@NotNull UUID uuid) { return getPlayState(uuid) != null; }

	public boolean hasPausedSong(@NotNull UUID uuid) {
		PlayState playState = getPlayState(uuid);
		return playState != null && playState.isPaused();
	}

	public @Nullable Song getPlayingSong(@NotNull UUID uuid) {
		PlayState playState = getPlayState(uuid);
		return playState != null ? playState.getSong() : null;
	}

	public @Nullable Song getNextSong(@NotNull Player player) {
		PlayState playState = getPlayState(player.getUniqueId());
		return playState != null ? getShuffleSong(player.getUniqueId(), playState.getSong()) : getRandomSong(player.getUniqueId());
	}

	public void stopSongs() {
		for(Map.Entry<UUID, PlayState> playState : playStates.entrySet()) {
			playState.getValue().getTimer().cancel();

			PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(playState.getKey());
			playSettings.setCurrentSong(null);

			Player player = Bukkit.getPlayer(playState.getKey());
			if(player != null && gMusicMain.getConfigService().A_SHOW_MESSAGES) gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-stop");
		}

		playStates.clear();
	}

	public void stopSong(@NotNull Player player) {
		PlayState playState = getPlayState(player.getUniqueId());
		if(playState == null) return;

		playState.getTimer().cancel();

		playStates.remove(player.getUniqueId());

		PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(player.getUniqueId());
		playSettings.setCurrentSong(null);

		if(gMusicMain.getConfigService().A_SHOW_MESSAGES) gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-stop");
	}

	public void pauseSong(@NotNull Player player) {
		PlayState playState = getPlayState(player.getUniqueId());
		if(playState == null) return;

		playState.getTimer().cancel();
		playState.setPaused(true);

		if(gMusicMain.getConfigService().A_SHOW_MESSAGES) gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-pause");
	}

	public void resumeSong(@NotNull Player player) {
		PlayState playState = getPlayState(player.getUniqueId());
		if(playState == null) return;

		playState.setTimer(new Timer());
		playState.setPaused(false);

		if(gMusicMain.getConfigService().A_SHOW_MESSAGES) gMusicMain.getMessageService().sendActionBarMessage(player, "Messages.actionbar-resume");

		startSong(player, playState.getSong(), playState.getTimer());
	}

}