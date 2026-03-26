package com.kntrel.mc.regionLib.region.display;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Player;

/**
 * Defines how a region is displayed.
 */
public interface RegionDisplayer {
    DisplayToken display(Region region);
    DisplayToken display(Region region, Player player);
    void stop(DisplayToken token);
}
