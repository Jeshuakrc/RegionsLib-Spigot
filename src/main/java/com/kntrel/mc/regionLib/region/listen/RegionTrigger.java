package com.kntrel.mc.regionLib.region.listen;

import com.kntrel.util.Priority;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

/**
 * Describes a signal raised when an {@link Event} happens inside a region.
 * A region trigger defines the event class to listen to, the condition that the
 * event must meet, how to localize where the event happened, and the priority
 * used to order triggers.
 *
 * <p>Listeners subscribe to one or more triggers and are invoked when a trigger
 * applies. See {@link RegionListener} for the listener contract.</p>
 *
 * @param <E> event type
 */
public interface RegionTrigger<E extends Event> extends Comparable<RegionTrigger<?>> {

    /**
     * Returns the {@link Event} class this trigger listens to.
     *
     * @return event class
     */
    Class<E> eventClass();

    /**
     * Returns the Bukkit listener priority used when registering for events.
     *
     * @return Bukkit priority
     */
    default EventPriority bukkitPriority() { return EventPriority.NORMAL; }

    /**
     * Returns the trigger ordering priority used to resolve competing triggers.
     *
     * @return trigger priority
     */
    Priority priority();

    /**
     * Determines where the event occurred (location or area) within the world,
     * so region lookups can be performed.
     *
     * @param event triggering event
     * @return localized place for the event
     */
    Place localize(E event);

    /**
     * Determines whether the event meets the trigger's condition.
     *
     * @param event triggering event
     * @return true if the event should signal this trigger
     */
    boolean appliesTo(E event);

    @Override default int compareTo(RegionTrigger<?> other) {
        return this.priority().compareTo(other.priority());
    }
}
