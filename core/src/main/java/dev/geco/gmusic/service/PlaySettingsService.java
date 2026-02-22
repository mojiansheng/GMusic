package dev.geco.gmusic.service;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.model.PlaySettings;
import dev.geco.gmusic.model.Song;
import dev.geco.gmusic.model.PlayListMode;
import dev.geco.gmusic.model.PlayMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlaySettingsService {

	private final GMusicMain gMusicMain;
	private final HashMap<UUID, PlaySettings> playSettingsCache = new HashMap<>();

	public PlaySettingsService(GMusicMain gMusicMain) {
		this.gMusicMain = gMusicMain;
	}

	public void createDataTables() {
		try {
			gMusicMain.getDataService().execute("CREATE TABLE IF NOT EXISTS gmusic_play_settings (uuid TEXT, playListMode INTEGER, volume INTEGER, playOnJoin INTEGER, playMode INTEGER, showParticles INTEGER, reverseMode INTEGER, toggleMode INTEGER, range INTEGER, currentSong TEXT);");
			gMusicMain.getDataService().execute("CREATE TABLE IF NOT EXISTS gmusic_play_settings_favorites (uuid TEXT, songId TEXT);");
		} catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not create play settings database tables!", e); }
	}

	public @NotNull PlaySettings getPlaySettings(@NotNull UUID uuid) {
		if(playSettingsCache.containsKey(uuid)) return playSettingsCache.get(uuid);

		List<Song> favorites = new ArrayList<>();

		PlaySettings playSettings = null;

		try {
			try(ResultSet playSettingsFavoritesData = gMusicMain.getDataService().executeAndGet("SELECT * FROM gmusic_play_settings_favorites WHERE uuid = ?", uuid.toString())) {
				while(playSettingsFavoritesData.next()) {
					favorites.add(gMusicMain.getSongService().getSongById(playSettingsFavoritesData.getString("songId")));
				}
			}

			try(ResultSet playSettingsData = gMusicMain.getDataService().executeAndGet("SELECT * FROM gmusic_play_settings WHERE uuid = ?", uuid.toString())) {
				if(playSettingsData.next()) {
					playSettings = new PlaySettings(
							uuid,
							PlayListMode.byId(playSettingsData.getInt("playListMode")),
							playSettingsData.getInt("volume"),
							playSettingsData.getBoolean("playOnJoin"),
							PlayMode.byId(playSettingsData.getInt("playMode")),
							playSettingsData.getBoolean("showParticles"),
							playSettingsData.getBoolean("reverseMode"),
							playSettingsData.getBoolean("toggleMode"),
							playSettingsData.getLong("range"),
							playSettingsData.getString("currentSong"),
							favorites
					);
				}
			}
		} catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not load play settings", e); }

		if(playSettings == null) playSettings = generateDefaultPlaySettings(uuid);
		else playSettingsCache.put(uuid, playSettings);

		playSettings.setFavorites(favorites);

		return playSettings;
	}

	public @NotNull PlaySettings generateDefaultPlaySettings(@NotNull UUID uuid) {
		PlaySettings playSettings = new PlaySettings(
				uuid,
				PlayListMode.byId(gMusicMain.getConfigService().PS_D_PLAYLIST_MODE),
				gMusicMain.getConfigService().PS_D_VOLUME,
				gMusicMain.getConfigService().R_PLAY_ON_JOIN,
				PlayMode.byId(gMusicMain.getConfigService().PS_D_PLAY_MODE),
				gMusicMain.getConfigService().PS_D_PARTICLES,
				gMusicMain.getConfigService().PS_D_REVERSE,
				false,
				0,
				null,
				new ArrayList<>()
		);

		playSettingsCache.put(uuid, playSettings);

		return playSettings;
	}

	public void savePlaySettings(@NotNull UUID uuid, @Nullable PlaySettings playSettings) {
		try {
			gMusicMain.getDataService().execute("DELETE FROM gmusic_play_settings WHERE uuid = ?", uuid.toString());
			gMusicMain.getDataService().execute("DELETE FROM gmusic_play_settings_favorites WHERE uuid = ?", uuid.toString());

			if(playSettings == null) {
				playSettingsCache.remove(uuid);
				return;
			}

			playSettingsCache.put(uuid, playSettings);

			gMusicMain.getDataService().execute("INSERT INTO gmusic_play_settings (uuid, playListMode, volume, playOnJoin, playMode, showParticles, reverseMode, toggleMode, range, currentSong) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					uuid.toString(),
					playSettings.getPlayListMode().getId(),
					playSettings.getVolume(),
					playSettings.isPlayOnJoin(),
					playSettings.getPlayMode().getId(),
					playSettings.isShowingParticles(),
					playSettings.isReverseMode(),
					playSettings.isToggleMode(),
					playSettings.getRange(),
					playSettings.getCurrentSong()
			);

			if(playSettings.getFavorites().isEmpty()) return;

			for(Song favoriteSong : playSettings.getFavorites()) {
				gMusicMain.getDataService().execute("INSERT INTO gmusic_play_settings_favorites (uuid, songId) VALUES (?, ?)", uuid.toString(), favoriteSong.getId());
			}
		} catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not save play settings", e); }
	}

	public void savePlaySettings() {
		for(Map.Entry<UUID, PlaySettings> playSettings : playSettingsCache.entrySet()) {
			savePlaySettings(playSettings.getKey(), playSettings.getValue());
		}

		playSettingsCache.clear();
	}

	public void removePlaySettingsCache(@NotNull UUID uuid) { playSettingsCache.remove(uuid); }

}