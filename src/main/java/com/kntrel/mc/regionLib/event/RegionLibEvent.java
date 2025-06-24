package com.kntrel.mc.regionLib.event;

import org.bukkit.plugin.Plugin;
import javax.annotation.Nonnull;


public interface RegionLibEvent {
    static void enable(@Nonnull Plugin plugin) {
        EventTriggerer.enable(plugin);
    }
}
