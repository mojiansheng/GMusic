package dev.geco.gmusic.link;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.link.worldguard.RegionFlagHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class WorldGuardLink {

    public static final StringFlag MUSIC_FLAG = new StringFlag("music");
    public static final StateFlag JUKEBOX_FLAG = new StateFlag("jukebox", true);

    public void registerFlags() {
        HashMap<String, Flag<?>> flags = new HashMap<>();
        flags.put(MUSIC_FLAG.getName(), MUSIC_FLAG);
        flags.put(JUKEBOX_FLAG.getName(), JUKEBOX_FLAG);
        FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();
        for(Map.Entry<String, Flag<?>> flag : flags.entrySet()) {
            try {
                flagRegistry.register(flag.getValue());
            } catch(Throwable ignored) { }
        }
    }

    public void registerFlagHandlers() {
        WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(RegionFlagHandler.FACTORY, null);
    }

    public void unregisterFlagHandlers() {
        WorldGuard.getInstance().getPlatform().getSessionManager().unregisterHandler(RegionFlagHandler.FACTORY);
    }

    public boolean canUseJukeboxInLocation(Location location, Player player) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if(container.get(BukkitAdapter.adapt(location.getWorld())) == null) return true;
            RegionQuery regionQuery = container.createQuery();
            com.sk89q.worldedit.util.Location regionLocation = BukkitAdapter.adapt(location);
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            return regionQuery.testState(regionLocation, localPlayer, JUKEBOX_FLAG, Flags.CHEST_ACCESS);
        } catch(Throwable e) { GMusicMain.getInstance().getLogger().log(Level.SEVERE, "Could not check WorldGuard location!", e); }
        return true;
    }

}