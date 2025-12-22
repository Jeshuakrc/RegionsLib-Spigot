package com.kntrel.mc.regionLib.event;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;


public interface RegionLibEvent {
    static void enable(@NotNull Plugin plugin) {
        EventTriggerer.enable(plugin);
    }
}
