package dev.geco.gmusic.util;

import dev.geco.gmusic.GMusicMain;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentUtil {

    private final GMusicMain gMusicMain;

    public EnvironmentUtil(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    private final List<Material> WATER_MATERIALS = new ArrayList<>(); {
        WATER_MATERIALS.add(Material.KELP_PLANT);
        WATER_MATERIALS.add(Material.SEAGRASS);
        WATER_MATERIALS.add(Material.TALL_SEAGRASS);
    }

    public boolean isPlayerSwimming(@NotNull Player player) {
        Block block = player.getEyeLocation().getBlock();
        if(block.isLiquid()) return true;
        if(block.getBlockData() instanceof Waterlogged && ((Waterlogged) block.getBlockData()).isWaterlogged()) return true;
        return WATER_MATERIALS.contains(block.getType());
    }

    public boolean isEntityInAllowedWorld(@NotNull Entity entity) {
        boolean allowed = !gMusicMain.getConfigService().WORLDBLACKLIST.contains(entity.getWorld().getName());
        if(!gMusicMain.getConfigService().WORLDWHITELIST.isEmpty() && !gMusicMain.getConfigService().WORLDWHITELIST.contains(entity.getWorld().getName())) allowed = false;
        return allowed || gMusicMain.getPermissionService().hasPermission(entity, "ByPass.World", "ByPass.*");
    }

    public boolean canUseJukeboxInLocation(@NotNull Location location, @NotNull Player player) {
        if(!gMusicMain.getConfigService().TRUSTED_REGION_ONLY || gMusicMain.getPermissionService().hasPermission(player, "ByPass.Region", "ByPass.*")) return true;
        if(gMusicMain.getPlotSquaredLink() != null && !gMusicMain.getPlotSquaredLink().canUseInLocation(location, player)) return false;
        if(gMusicMain.getWorldGuardLink() != null && !gMusicMain.getWorldGuardLink().canUseJukeboxInLocation(location, player)) return false;
        if(gMusicMain.getGriefPreventionLink() != null && !gMusicMain.getGriefPreventionLink().canUseInLocation(location, player)) return false;
        return true;
    }

}