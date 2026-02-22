package dev.geco.gmusic.cmd.tab;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.model.Song;
import dev.geco.gmusic.service.SongService;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GAdminMusicTabComplete implements TabCompleter {

    private final GMusicMain gMusicMain;

    public GAdminMusicTabComplete(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> complete = new ArrayList<>(), completeStarted = new ArrayList<>();
        if(!(sender instanceof Player)) return complete;

        if(args.length == 1) {
            if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic", "AdminMusic.*")) {
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.JukeBox", "AdminMusic.*")) complete.add("jukebox");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Disc", "AdminMusic.*")) complete.add("disc");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Download", "AdminMusic.*")) complete.add("download");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Radio", "AdminMusic.*")) complete.add("radio");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Edit", "AdminMusic.*")) complete.add("edit");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Play", "AdminMusic.*")) complete.add("play");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Playing", "AdminMusic.*")) complete.add("playing");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Random", "AdminMusic.*")) complete.add("random");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Stop", "AdminMusic.*")) complete.add("stop");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Pause", "AdminMusic.*")) complete.add("pause");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Resume", "AdminMusic.*")) complete.add("resume");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Skip", "AdminMusic.*")) complete.add("skip");
                if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic.Toggle", "AdminMusic.*")) complete.add("toggle");
            }
            if(!args[args.length - 1].isEmpty()) {
                for(String entry : complete) if(entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) completeStarted.add(entry);
                complete.clear();
            }
        } else if(args.length == 2) {
            if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic", "AdminMusic.*") && gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic." + args[0], "AdminMusic.*")) {
                switch(args[0].toLowerCase()) {
                    case "jukebox":
                    case "play":
                    case "playing":
                    case "random":
                    case "stop":
                    case "pause":
                    case "resume":
                    case "skip":
                    case "toggle":
                        for(Player player : Bukkit.getOnlinePlayers()) complete.add(player.getName());
                        break;
                    case "disc":
                    case "edit":
                        for(Song song : gMusicMain.getSongService().getSongs()) complete.add(song.getId());
                        break;
                    case "download":
                        complete.add(SongService.GNBS_EXTENSION);
                        complete.add(SongService.NBS_EXTENSION);
                        complete.add(SongService.MID_EXTENSION);
                        complete.add(SongService.MIDI_EXTENSION);
                        break;
                }
            }
            if(!args[args.length - 1].isEmpty()) {
                for(String entry : complete) if(entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) completeStarted.add(entry);
                complete.clear();
            }
        } else if(args.length == 3) {
            if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic", "AdminMusic.*") && gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic." + args[0], "AdminMusic.*")) {
                if(args[0].equalsIgnoreCase("disc")) {
                    for(Player player : Bukkit.getOnlinePlayers()) complete.add(player.getName());
                } else if(args[0].equalsIgnoreCase("edit")) {
                    complete.add("id");
                    complete.add("title");
                    complete.add("original_author");
                    complete.add("author");
                    complete.add("description");
                    complete.add("category");
                } else if(args[0].equalsIgnoreCase("play")) {
                    for(Song song : gMusicMain.getSongService().getSongs()) complete.add(song.getId());
                }
            }
            if(!args[args.length - 1].isEmpty()) {
                for(String entry : complete) if(entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) completeStarted.add(entry);
                complete.clear();
            }
        } else if(args.length == 4) {
            if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic", "AdminMusic.*") && gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic." + args[0], "AdminMusic.*")) {
                if(args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("category")) {
                    for(SoundCategory category : SoundCategory.values()) complete.add(category.name().toLowerCase());
                }
            }
            if(!args[args.length - 1].isEmpty()) {
                for(String entry : complete) if(entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) completeStarted.add(entry);
                complete.clear();
            }
        }
        return complete.isEmpty() ? completeStarted : complete;
    }

}