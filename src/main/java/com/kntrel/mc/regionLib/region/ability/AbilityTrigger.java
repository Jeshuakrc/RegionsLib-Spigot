package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Trigger definition for abilities, extending region triggers with player attribution.
 *
 * <p>Ability triggers describe the event to listen to, the condition for signaling,
 * how to localize where the event happened, and how to attribute the event to a
 * player so an {@link Ability} can evaluate permissions.</p>
 *
 * @param <E> event type
 */
public interface AbilityTrigger<E extends Event> extends RegionTrigger<E> {

    /**
     * Extracts the player attributed to the event.
     *
     * @param event triggering event
     * @return attributed player (may be null if no player can be attributed)
     */
    Player attribute(E event);
}
