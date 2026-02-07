package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public interface AbilityTrigger<E extends Event> extends RegionTrigger<E> {

    Player attribute(E event);
}
