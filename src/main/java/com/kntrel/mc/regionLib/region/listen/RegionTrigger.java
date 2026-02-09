package com.kntrel.mc.regionLib.region.listen;

import com.kntrel.util.Priority;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

/**
 * Defines a signal to be raised when a Bukkit event happens inside a region.
 * <p>
 * A trigger describes which {@link Event} class to listen to, how to localize
 * the event into a {@link Place} (point or area), the condition that must be
 * satisfied for it to apply, and the priority used for ordering/dispatch.
 *
 * @param <E> event type
 */
public interface RegionTrigger<E extends Event> extends Comparable<RegionTrigger<?>> {

    /**
     * Returns the event class this trigger listens for.
     *
     * @return event class
     */
    Class<E> eventClass();

    /**
     * Returns the Bukkit event priority for registration.
     *
     * @return event priority
     */
    default EventPriority bukkitPriority() { return EventPriority.NORMAL; }

    /**
     * Returns the internal trigger priority used for ordering.
     *
     * @return trigger priority
     */
    Priority priority();

    /**
     * Determines where the event took place.
     *
     * @param event event instance
     * @return localized place
     */
    Place localize(E event);

    /**
     * Returns whether the event matches this trigger's condition.
     *
     * @param event event instance
     * @return true if the trigger should apply
     */
    boolean appliesTo(E event);

    @Override default int compareTo(RegionTrigger<?> other) {
        return this.priority().compareTo(other.priority());
    }
}
