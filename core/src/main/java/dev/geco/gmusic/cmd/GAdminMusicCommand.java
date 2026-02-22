package dev.geco.gmusic.cmd;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.model.Song;
import dev.geco.gmusic.model.gui.MusicGUI;
import dev.geco.gmusic.service.SongService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;

public class GAdminMusicCommand implements CommandExecutor {

    private final GMusicMain gMusicMain;

    public GAdminMusicCommand(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(!gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic")) {
            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-permission-error");
            return true;
        }

        if(args.length == 0) {
            gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-use-error");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "jukebox" -> {
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
                if(args.length == 1) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gadminmusic-disc-use-error");
                    return true;
                }
                Song song = gMusicMain.getSongService().getSongById(args[1]);
                if(song == null) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-gmusic-song-error", "%Song%", args[1]);
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
                if(!(sender instanceof Player player)) {
                    gMusicMain.getMessageService().sendMessage(sender, "Messages.command-sender-error");
                    return true;
                }
                MusicGUI musicGUI = MusicGUI.getMusicGUI(gMusicMain.getRadioService().getRadioUUID());
                if(musicGUI != null) player.openInventory(musicGUI.getInventory());
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