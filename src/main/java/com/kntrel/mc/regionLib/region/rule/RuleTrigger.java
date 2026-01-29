package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import org.bukkit.event.Event;

public interface RuleTrigger<T, E extends Event> extends RegionTrigger<E> {

    boolean test(T value, Region region, E event);

    void fire(T value, Region region, E event);

}
