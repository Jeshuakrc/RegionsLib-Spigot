package com.kntrel.mc.regionLib.region.listen;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.util.Collection;

/**
 * Listener that subscribes to one or more {@link RegionTrigger} instances.
 * Each listener is identified by a string name and reacts when any of its
 * triggers signal within a region.
 *
 * <p>Specialized listeners include abilities and rules, which interpret trigger
 * signals differently. See {@link com.kntrel.mc.regionLib.region.ability.Ability}
 * and {@link com.kntrel.mc.regionLib.region.rule.Rule} for higher-level behavior.</p>
 *
 * @param <T> trigger type
 */
public interface RegionListener<T extends RegionTrigger<?>> {

    /**
     * Returns the identifier for this listener.
     *
     * @return listener name
     */
    String name();
    /**
     * Returns the triggers this listener subscribes to.
     *
     * @return trigger collection
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
