package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Trigger interface for abilities that can attribute a player.
 *
 * @param <E> event type
 */
public interface AbilityTrigger<E extends Event> extends RegionTrigger<E> {

    /**
     * Extracts the player attributed to the event.
     *
     * @param event triggering event
     * @return attributed player
     */
    Player attribute(E event);
}
