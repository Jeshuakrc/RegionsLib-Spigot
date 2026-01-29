package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public interface AbilityTrigger<E extends Event> extends RegionTrigger<E> {

    boolean test(Region region, E event);

    Player attribute(E event);

    void onAllowed(Region region, E event);

    void onDenied(Region region, E event);

}
