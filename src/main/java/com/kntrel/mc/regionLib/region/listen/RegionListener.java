package com.kntrel.mc.regionLib.region.listen;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.util.Collection;

/**
 * A named subscriber to one or more {@link RegionTrigger} instances.
 * <p>
 * Region listeners are identified by a stable string name and provide a
 * collection of triggers that determine which events should be routed to
 * the listener.
 *
 * @param <T> trigger type
 */
public interface RegionListener<T extends RegionTrigger<?>> {

    /**
     * Returns the unique name for this listener.
     *
     * @return listener name
     */
    String name();
    /**
     * Returns the triggers this listener subscribes to.
     *
     * @return collection of triggers
     */
    Collection<T> triggers();


    // UTILITY
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static boolean triggersOn(RegionListener<?> listener, Event event) {
        for (RegionTrigger trigger : listener.triggers()) {
            if (!trigger.eventClass().isInstance(event)) { continue; }
            try {
                if (trigger.appliesTo(event)) { return true; }
            } catch (ClassCastException ignored) {}
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static boolean triggersOn (RegionListener<?> listener, Event event, EventPriority priority) {
        for (RegionTrigger trigger : listener.triggers()) {
            if (!trigger.eventClass().isInstance(event) || !trigger.bukkitPriority().equals(priority)) {
                continue;
            }
            try {
                if (trigger.appliesTo(event)) { return true; }
            } catch (ClassCastException ignored) {}
        }
        return false;
    }
}
