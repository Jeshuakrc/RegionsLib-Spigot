package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.listen.RegionListener;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.List;
import java.util.Optional;

/**
 * Models a permission-style reaction for player-caused events inside regions.
 *
 * <p>An ability listens to {@link AbilityTrigger} instances, which are specialized
 * {@link com.kntrel.mc.regionLib.region.listen.RegionTrigger} definitions that
 * can attribute the event to a {@link org.bukkit.entity.Player}. Abilities answer
 * the question: "When something happens inside a region, and it was caused by a
 * player, is the player allowed to do it?"</p>
 *
 * <p>When allowed, {@link #onAllowed(Event, List)} is invoked. When denied,
 * {@link #onDenied(Event, List)} is invoked (defaults to cancelling the event if
 * it is {@link Cancellable}). For examples and registration, see
 * {@link com.kntrel.mc.regionLib.provided.Abilities}.</p>
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
