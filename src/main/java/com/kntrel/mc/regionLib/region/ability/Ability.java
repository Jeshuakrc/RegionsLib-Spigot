package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.listen.RegionListener;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.List;
import java.util.Optional;

/**
 * Defines a region ability and its listener behavior.
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
     * Called when the ability is allowed.
     *
     * @param event triggering event
     * @param regions regions evaluated
     */
    default void onAllowed(Event event, List<Region> regions) { /* nothing */ }
    /**
     * Called when the ability is denied.
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
