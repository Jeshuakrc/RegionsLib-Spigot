package com.kntrel.mc.regionLib.region.listen;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.util.Collection;

/**
 * Listener for region triggers.
 *
 * @param <T> trigger type
 */
public interface RegionListener<T extends RegionTrigger<?>> {

    String name();
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
