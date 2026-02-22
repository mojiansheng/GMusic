package dev.geco.gmusic.cmd.tab;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.model.Song;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GMusicTabComplete implements TabCompleter {

    private final GMusicMain gMusicMain;

    public GMusicTabComplete(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> complete = new ArrayList<>(), completeStarted = new ArrayList<>();
        if(!(sender instanceof Player)) return complete;

        if(args.length == 1) {
            if(gMusicMain.getPermissionService().hasPermission(sender, "Music", "Music.*")) {
                if(gMusicMain.getPermissionService().hasPermission(sender, "Music.Play", "Music.*")) complete.add("play");
                if(gMusicMain.getPermissionService().hasPermission(sender, "Music.Playing", "Music.*")) complete.add("playing");
                if(gMusicMain.getPermissionService().hasPermission(sender, "Music.Random", "Music.*")) complete.add("random");
                if(gMusicMain.getPermissionService().hasPermission(sender, "Music.Stop", "Music.*")) complete.add("stop");
                if(gMusicMain.getPermissionService().hasPermission(sender, "Music.Pause", "Music.*")) complete.add("pause");
                if(gMusicMain.getPermissionService().hasPermission(sender, "Music.Resume", "Music.*")) complete.add("resume");
                if(gMusicMain.getPermissionService().hasPermission(sender, "Music.Skip", "Music.*")) complete.add("skip");
                if(gMusicMain.getPermissionService().hasPermission(sender, "Music.Toggle", "Music.*")) complete.add("toggle");
            }
            if(!args[args.length - 1].isEmpty()) {
                for(String entry : complete) if(entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) completeStarted.add(entry);
                complete.clear();
            }
        } else if(args.length == 2) {
            if(gMusicMain.getPermissionService().hasPermission(sender, "Music", "Music.*") && gMusicMain.getPermissionService().hasPermission(sender, "Music." + args[0], "Music.*")) {
                if(args[0].equalsIgnoreCase("play")) {
                    for(Song song : gMusicMain.getSongService().getSongs()) complete.add(song.getId());
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