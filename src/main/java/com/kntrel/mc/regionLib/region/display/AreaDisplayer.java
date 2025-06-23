package com.kntrel.mc.regionLib.region.display;

import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.entity.Player;

public interface AreaDisplayer {
    DisplayToken display(Area area);
    DisplayToken display(Area area, Player player);
    void stop(DisplayToken token);
}
