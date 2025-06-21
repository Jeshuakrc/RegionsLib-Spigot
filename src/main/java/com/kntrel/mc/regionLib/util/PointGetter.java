package com.kntrel.mc.regionLib.util;

import org.bukkit.Location;
import org.bukkit.event.Event;
import java.util.function.Function;

public interface PointGetter extends Function<Event, Location> {
}
