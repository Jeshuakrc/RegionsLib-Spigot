package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * A {@link RegionTrigger} that attributes an event to a player.
 * <p>
 * Ability triggers are used by {@link Ability} to determine the player who
 * caused the event so that region permissions can be checked.
 *
 * @param <E> event type
 */
public interface AbilityTrigger<E extends Event> extends RegionTrigger<E> {

    /**
     * Extracts the player attributed to the event.
     *
     * @param event triggering event
     * @return attributed player, or null if none
     */
    Player attribute(E event);
}
