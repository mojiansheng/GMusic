package dev.geco.gmusic.link.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.link.WorldGuardLink;
import dev.geco.gmusic.object.GSong;
import org.bukkit.entity.Player;

import java.util.Set;

public class RegionFlagHandler extends Handler {

    public static final Factory FACTORY = new Factory();
    public static class Factory extends Handler.Factory<RegionFlagHandler> {
        @Override
        public RegionFlagHandler create(Session session) {
            return new RegionFlagHandler(session);
        }
    }

    private final GMusicMain gMusicMain;
    private String currentId = null;

    public RegionFlagHandler(Session session) {
        super(session);
        gMusicMain = GMusicMain.getInstance();
    }

    @Override
    public boolean onCrossBoundary(LocalPlayer localPlayer, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        String songId = toSet.queryValue(localPlayer, WorldGuardLink.MUSIC_FLAG);
        if(songId == null || songId.isEmpty()) {
            currentId = null;
            return true;
        }

        if(songId.equals(currentId)) return true;
        currentId = songId;

        GSong song = gMusicMain.getSongService().getSongById(songId);
        if(song == null) return true;

        Player player = BukkitAdapter.adapt(localPlayer);
        gMusicMain.getPlayService().playSong(player, song);

        return true;
    }

}