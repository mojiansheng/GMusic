package dev.geco.gmusic.link;

import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import dev.geco.gmusic.GMusicMain;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class PlotSquaredLink {

    private final GMusicMain gMusicMain;

    public PlotSquaredLink(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    public boolean canUseInLocation(Location Location, Player Player) {
        try {
            PlotAPI plotAPI = new PlotAPI();

            PlotPlayer<?> plotPlayer = plotAPI.wrapPlayer(Player.getUniqueId());
            if(plotPlayer == null) return true;

            com.plotsquared.core.location.Location location = com.plotsquared.core.location.Location.at(plotPlayer.getLocation().getWorld(), Location.getBlockX(), Location.getBlockY(), Location.getBlockZ());

            PlotArea plotArea = plotAPI.getPlotSquared().getPlotAreaManager().getApplicablePlotArea(location);
            if(plotArea == null) return true;

            Plot plot = plotArea.getOwnedPlot(location);
            if(plot == null) return true;

            return !plot.isDenied(Player.getUniqueId()) && plot.isAdded(Player.getUniqueId());
        } catch(Throwable e) { gMusicMain.getLogger().log(Level.SEVERE, "Could not check PlotSquared location!", e); }
        return true;
    }

    public boolean isPlotSquaredVersionSupported() {
        try {
            new PlotAPI();
            return true;
        } catch(Throwable ignored) { }
        return false;
    }

}