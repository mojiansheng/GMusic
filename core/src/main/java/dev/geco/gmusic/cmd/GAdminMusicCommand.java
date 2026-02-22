package dev.geco.gmusic.cmd;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.model.PlaySettings;
import dev.geco.gmusic.model.Song;
import dev.geco.gmusic.model.gui.MusicGUI;
import dev.geco.gmusic.service.SongService;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class GAdminMusicCommand implements CommandExecutor {

    private final GMusicMain gMusicMain;

    public GAdminMusicCommand(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic", "AdminMusic.*")) {
            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
            return true;
        }

        if(args.length == 0) {
            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-use-error");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "jukebox" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.JukeBox", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                Player target = null;
                if(!(sender instanceof Player)) {
                    if(args.length == 1) {
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-jukebox-use-error");
                        return true;
                    }
                } else target = (Player) sender;
                if(args.length > 1) {
                    target = getPlayer(args[1]);
                    if(target == null) {
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                        return true;
                    }
                }
                int amount = 1;
                if(args.length > 2) {
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch(NumberFormatException e) {
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-amount-error", "%Amount%", args[2]);
                    }
                }
                ItemStack jukeBox = gMusicMain.getJukeBoxService().createJukeBoxItem();
                jukeBox.setAmount(amount);
                target.getInventory().addItem(jukeBox);
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-jukebox", "%Player%", target.getName(), "%Amount%", amount);
            }
            case "disc" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Disc", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-disc-use-error");
                    return true;
                }
                Song song = gMusicMain.getSongService().getSongById(args[1]);
                if(song == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-song-error", "%Song%", args[1]);
                    return true;
                }
                Player target = null;
                if(!(sender instanceof Player)) {
                    if(args.length == 2) {
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-jukebox-use-error");
                        return true;
                    }
                } else target = (Player) sender;
                if(args.length > 2) {
                    target = getPlayer(args[2]);
                    if(target == null) {
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[2]);
                        return true;
                    }
                }
                int amount = 1;
                if(args.length > 3) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch(NumberFormatException e) {
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-amount-error", "%Amount%", args[3]);
                    }
                }
                ItemStack disc = gMusicMain.getDiscService().createDiscItem(song);
                disc.setAmount(amount);
                target.getInventory().addItem(disc);
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-disc", "%Player%", target.getName(), "%Amount%", amount);
            }
            case "download" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Download", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length <= 3) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-download-use-error");
                    return true;
                }
                String extension = args[1].toLowerCase();
                String filename = args[2];
                String url = args[3];
                switch(extension) {
                    case SongService.GNBS_EXTENSION: {
                        File file = new File(gMusicMain.getDataFolder(), SongService.GNBS_FOLDER + "/" + filename + "." + extension);
                        if(file.exists()) {
                            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-download-filename-error", "%Filename%", filename);
                            return true;
                        }
                        boolean success = downloadFile(url, file.toPath());
                        if(success) success = gMusicMain.getSongService().loadSongFile(file);
                        if(!success) {
                            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-download-error");
                            return true;
                        }
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-download", "%Filename%", filename);
                    }
                    case SongService.NBS_EXTENSION:
                    case SongService.MID_EXTENSION:
                    case SongService.MIDI_EXTENSION:
                        File file = new File(gMusicMain.getDataFolder(), SongService.CONVERT_FOLDER + "/" + filename + "." + extension);
                        if(file.exists()) {
                            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-download-filename-error", "%Filename%", filename);
                            return true;
                        }
                        boolean success = downloadFile(url, file.toPath());
                        if(success) {
                            file = gMusicMain.getSongService().convertSongFile(file);
                            if(file == null) success = false;
                        }
                        if(success) success = gMusicMain.getSongService().loadSongFile(file);
                        if(!success) {
                            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-download-error");
                            return true;
                        }
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-download", "%Filename%", filename);
                        break;
                    default: {
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-download-type-error", "%Type%", args[1]);
                        return true;
                    }
                }
            }
            case "radio" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Radio", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(!(sender instanceof Player player)) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-sender-error");
                    return true;
                }
                MusicGUI musicGUI = MusicGUI.getMusicGUI(gMusicMain.getRadioService().getRadioUUID());
                if(musicGUI != null) player.openInventory(musicGUI.getInventory());
            }
            case "edit" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Edit", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                Song song = gMusicMain.getSongService().getSongById(args[1]);
                if(song == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-song-error", "%Song%", args[1]);
                    return true;
                }
                switch(args[2].toLowerCase()) {
                    case "id" -> {
                        try {
                            if(gMusicMain.getSongService().getSongs().stream().anyMatch(s -> s.getId().equalsIgnoreCase(args[3]))) {
                                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-edit-id-error", "%Song%", args[3]);
                                return true;
                            }
                            File file = new File(song.getFileName());
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                            config.set("Song.Id", args[3]);
                            config.save(file);
                            gMusicMain.getSongService().loadSongs();
                        } catch(Throwable e) {
                            gMusicMain.getLogger().log(Level.SEVERE, "Error saving song", e);
                        }
                    }
                    case "title" -> {
                        try {
                            File file = new File(song.getFileName());
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                            config.set("Song.Title", args[3]);
                            config.save(file);
                            gMusicMain.getSongService().loadSongs();
                        } catch(Throwable e) {
                            gMusicMain.getLogger().log(Level.SEVERE, "Error saving song", e);
                        }
                    }
                    case "original_author" -> {
                        try {
                            File file = new File(song.getFileName());
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                            config.set("Song.OriginalAuthor", args[3]);
                            config.save(file);
                            gMusicMain.getSongService().loadSongs();
                        } catch(Throwable e) {
                            gMusicMain.getLogger().log(Level.SEVERE, "Error saving song", e);
                        }
                    }
                    case "author" -> {
                        try {
                            File file = new File(song.getFileName());
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                            config.set("Song.Author", args[3]);
                            config.save(file);
                            gMusicMain.getSongService().loadSongs();
                        } catch(Throwable e) {
                            gMusicMain.getLogger().log(Level.SEVERE, "Error saving song", e);
                        }
                    }
                    case "description" -> {
                        try {
                            File file = new File(song.getFileName());
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                            List<String> description = new ArrayList<>();
                            description.add(args[3]);
                            config.set("Song.Description", description);
                            config.save(file);
                            gMusicMain.getSongService().loadSongs();
                        } catch(Throwable e) {
                            gMusicMain.getLogger().log(Level.SEVERE, "Error saving song", e);
                        }
                    }
                    case "category" -> {
                        try {
                            SoundCategory category = SoundCategory.valueOf(args[3].toUpperCase());
                            try {
                                File file = new File(song.getFileName());
                                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                                config.set("Song.Category", category.name().toUpperCase());
                                config.save(file);
                                gMusicMain.getSongService().loadSongs();
                            } catch(Throwable e) {
                                gMusicMain.getLogger().log(Level.SEVERE, "Error saving song", e);
                            }
                        } catch(Throwable e) {
                            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-edit-category-error", "%Category%", args[3]);
                        }
                    }
                    default -> {
                        gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-edit-option-error", "%Option%", args[2]);
                    }
                }
            }
            case "play" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Play", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length <= 2) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-play-use-error");
                    return true;
                }
                Player target = getPlayer(args[1]);
                if(target == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                    return true;
                }
                Song song = gMusicMain.getSongService().getSongById(args[2]);
                if(song == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-song-error", "%Song%", args[2]);
                    return true;
                }
                gMusicMain.getPlayService().playSong(target, song);
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-play", "%Song%", song.getId(), "%SongTitle%", song.getTitle());
            }
            case "playing" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Playing", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-playing-use-error");
                    return true;
                }
                Player target = getPlayer(args[1]);
                if(target == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                    return true;
                }
                if(!gMusicMain.getPlayService().hasPlayingSong(target.getUniqueId()) || gMusicMain.getPlayService().hasPausedSong(target.getUniqueId())) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-playing-error");
                    return true;
                }
                Song song = gMusicMain.getPlayService().getPlayingSong(target.getUniqueId());
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-playing", "%Song%", song.getId(), "%SongTitle%", song.getTitle());
            }
            case "random" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Random", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-random-use-error");
                    return true;
                }
                Player target = getPlayer(args[1]);
                if(target == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                    return true;
                }
                Song song = gMusicMain.getPlayService().getRandomSong(target.getUniqueId());
                if(song == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-no-song-error");
                    return true;
                }
                gMusicMain.getPlayService().playSong(target, song);
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-play", "%Song%", song.getId(), "%SongTitle%", song.getTitle());
            }
            case "stop" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Stop", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-stop-use-error");
                    return true;
                }
                Player target = getPlayer(args[1]);
                if(target == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                    return true;
                }
                if(!gMusicMain.getPlayService().hasPlayingSong(target.getUniqueId())) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-playing-error");
                    return true;
                }
                gMusicMain.getPlayService().stopSong(target);
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-stop");
            }
            case "pause" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Pause", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-pause-use-error");
                    return true;
                }
                Player target = getPlayer(args[1]);
                if(target == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                    return true;
                }
                if(!gMusicMain.getPlayService().hasPlayingSong(target.getUniqueId()) || gMusicMain.getPlayService().hasPausedSong(target.getUniqueId())) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-playing-error");
                    return true;
                }
                gMusicMain.getPlayService().pauseSong(target);
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-pause");
            }
            case "resume" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Resume", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-resume-use-error");
                    return true;
                }
                Player target = getPlayer(args[1]);
                if(target == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                    return true;
                }
                if(!gMusicMain.getPlayService().hasPausedSong(target.getUniqueId())) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-paused-error");
                    return true;
                }
                gMusicMain.getPlayService().resumeSong(target);
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-resume");
            }
            case "skip" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Skip", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-skip-use-error");
                    return true;
                }
                Player target = getPlayer(args[1]);
                if(target == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                    return true;
                }
                Song song = gMusicMain.getPlayService().getNextSong(target);
                if(song == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-no-song-error");
                    return true;
                }
                gMusicMain.getPlayService().playSong(target, song);
                gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-play", "%Song%", song.getId(), "%SongTitle%", song.getTitle());
            }
            case "toggle" -> {
                if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Toggle", "AdminMusic.*")) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
                    return true;
                }
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-toggle-use-error");
                    return true;
                }
                Player target = getPlayer(args[1]);
                if(target == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-target-error", "%Target%", args[1]);
                    return true;
                }
                PlaySettings playSettings = gMusicMain.getPlaySettingsService().getPlaySettings(target.getUniqueId());
                playSettings.setToggleMode(!playSettings.isToggleMode());
            }
            default -> gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-use-error");
        }

        return true;
    }

    private Player getPlayer(String source) {
        try {
            UUID uuid = UUID.fromString(source);
            return Bukkit.getPlayer(uuid);
        } catch(IllegalArgumentException e) {
            return Bukkit.getPlayer(source);
        }
    }

    private boolean downloadFile(String source, Path destination) {
        try {
            try(InputStream in = new URL(source).openStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch(Throwable e) {
            gMusicMain.getLogger().log(Level.WARNING, "Downloading file failed", e);
            return false;
        }
    }

}