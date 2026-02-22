package dev.geco.gmusic.service;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.object.GSong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;

public class SongService {

    public static final String GNBS_EXTENSION = "gnbs";
    public static final String NBS_EXTENSION = "nbs";
    public static final String MIDI_EXTENSION = "midi";
    public static final String MID_EXTENSION = "mid";
    public static final String GNBS_FOLDER = "gnbs";
    public static final String CONVERT_FOLDER = "convert";

    private final GMusicMain gMusicMain;
    private final TreeMap<String, GSong> songs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public SongService(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    public List<GSong> getSongs() { return new ArrayList<>(songs.values()); }

    public @Nullable GSong getSongById(@NotNull String song) { return songs.get(song.toLowerCase()); }

    public List<GSong> filterSongsBySearch(@NotNull List<GSong> songs, @NotNull String search) { return songs.stream().filter(song -> song.getTitle().toLowerCase().contains(search.toLowerCase())).toList(); }

    public void loadSongs() {
        unloadSongs();

        convertSongs();

        File gnbsDir = new File(gMusicMain.getDataFolder(), GNBS_FOLDER);
        if(!gnbsDir.exists()) return;

        File[] songFiles = gnbsDir.listFiles();
        if(songFiles == null) return;
        for(File file : songFiles) loadSongFile(file);
    }

    public boolean loadSongFile(@NotNull File file) {
        int extensionPos = file.getName().lastIndexOf(".");
        if(extensionPos <= 0 || !file.getName().substring(extensionPos + 1).equalsIgnoreCase(GNBS_EXTENSION)) return false;

        try {
            GSong song = new GSong(file);
            if(song.getNoteAmount() == 0) {
                gMusicMain.getLogger().warning("Could not load " + GNBS_EXTENSION + " music '" + file.getName().substring(0, extensionPos) + "', no notes found");
                return false;
            }

            songs.put(song.getId().toLowerCase(), song);
            return true;
        } catch(Throwable e) {
            gMusicMain.getLogger().log(Level.WARNING, "Could not load " + GNBS_EXTENSION + " music '" + file.getName().substring(0, extensionPos) + "'", e);
            return false;
        }
    }

    public void unloadSongs() {
        songs.clear();
    }

    public void convertSongs() {
        File gnbsDir = new File(gMusicMain.getDataFolder(), GNBS_FOLDER);
        if(!gnbsDir.exists() && !gnbsDir.mkdir()) {
            gMusicMain.getLogger().severe("Could not create '" + GNBS_FOLDER + "' directory!");
            return;
        }

        File convertDir = new File(gMusicMain.getDataFolder(), CONVERT_FOLDER);
        if(!convertDir.exists() && !convertDir.mkdir()) {
            gMusicMain.getLogger().severe("Could not create '" + CONVERT_FOLDER + "' directory!");
            return;
        }

        File[] convertFiles = convertDir.listFiles();
        if(convertFiles == null) return;

        for(File file : convertFiles) convertSongFile(file);
    }

    public @Nullable File convertSongFile(@NotNull File file) {
        File gnbsDir = new File(gMusicMain.getDataFolder(), GNBS_FOLDER);

        File gnbsFile = new File(gnbsDir.getAbsolutePath() + "/" + file.getName().replaceFirst("[.][^.]+$", "") + "." + GNBS_EXTENSION);
        if(gnbsFile.exists()) return gnbsFile;

        String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        switch(extension.toLowerCase()) {
            case NBS_EXTENSION: {
                if(gMusicMain.getNBSConverter().convertNBSFile(file)) return gnbsFile;
                return null;
            }
            case MID_EXTENSION:
            case MIDI_EXTENSION:
                if(gMusicMain.getMidiConverter().convertMidiFile(file)) return gnbsFile;
                return null;
            default:
                gMusicMain.getLogger().warning("Invalid convert extension: " + extension);
        }

        return null;
    }

}