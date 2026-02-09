package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.listen.RegionListener;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.List;
import java.util.Optional;

/**
 * A {@link com.kntrel.mc.regionLib.region.listen.RegionListener} that models
 * player-caused actions inside regions.
 * <p>
 * Abilities answer: "When something happens inside a region and was caused by a
 * player, does the player have permission to do so?" They listen to
 * {@link AbilityTrigger} instances (which attribute an event to a player) and
 * define what to do when the action is allowed or denied. The default behavior
 * for denied actions is to cancel the event if it is {@link Cancellable}.
 */
public interface Ability extends RegionListener<AbilityTrigger<?>> {

    //FACTORY
    /**
     * Creates a builder for an ability based on an event type.
     *
     * @param eventClass event class to listen to
     * @param <E> event type
     * @return ability builder
     */
    static <E extends Event> AbilityBuilder<E> on(Class<E> eventClass) {
        return AbilityBuilder.on(eventClass);
    }


    //CONTRACT
    /**
     * Returns a parent ability, if any.
     *
     * @return optional parent ability
     */
    default Optional<Ability> superAbility() { return Optional.empty(); }
    /**
     * Called when the ability is allowed in the evaluated regions.
     *
     * @param event triggering event
     * @param regions regions evaluated
     */
    default void onAllowed(Event event, List<Region> regions) { /* nothing */ }
    /**
     * Called when the ability is denied in the evaluated regions.
     * <p>
     * Default behavior cancels the event if possible.
     *
     * @param event triggering event
     * @param regions regions evaluated
     */
    default void onDenied(Event event, List<Region> regions) {
        if (event instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
    }
}
