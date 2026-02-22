package dev.geco.gmusic.model.gui;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface MusicInputGUI {

    void open(LivingEntity entity, String title, ItemStack inputItem);

    void close(boolean force);

    interface InputCallback { boolean call(String input); }

    interface ValidateCallback { String call(ItemMeta meta); }

}