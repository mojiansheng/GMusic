package dev.geco.gmusic.cmd.tab;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.object.GSong;
import dev.geco.gmusic.service.SongService;
import org.bukkit.Bukkit;
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
            if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic")) {
                complete.add("jukebox");
                complete.add("disc");
                complete.add("download");
                complete.add("radio");
            }
            if(!args[args.length - 1].isEmpty()) {
                for(String entry : complete) if(entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) completeStarted.add(entry);
                complete.clear();
            }
        } else if(args.length == 2) {
            if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic")) {
                if(args[0].equalsIgnoreCase("jukebox")) {
                    for(Player player : Bukkit.getOnlinePlayers()) complete.add(player.getName());
                } else if(args[0].equalsIgnoreCase("disc")) {
                    for(GSong song : gMusicMain.getSongService().getSongs()) complete.add(song.getId());
                } else if(args[0].equalsIgnoreCase("download")) {
                    complete.add(SongService.GNBS_EXTENSION);
                    complete.add(SongService.NBS_EXTENSION);
                    complete.add(SongService.MID_EXTENSION);
                    complete.add(SongService.MIDI_EXTENSION);
                }
            }
            if(!args[args.length - 1].isEmpty()) {
                for(String entry : complete) if(entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) completeStarted.add(entry);
                complete.clear();
            }
        } else if(args.length == 3) {
            if(gMusicMain.getPermissionService().hasPermission(sender, "AdminMusic")) {
                if(args[0].equalsIgnoreCase("disc")) {
                    for(Player player : Bukkit.getOnlinePlayers()) complete.add(player.getName());
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