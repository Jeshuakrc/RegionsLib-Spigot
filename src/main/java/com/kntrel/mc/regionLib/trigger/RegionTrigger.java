package com.kntrel.mc.regionLib.trigger;

import com.kntrel.util.Priority;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

public interface RegionTrigger<E extends Event> extends Comparable<RegionTrigger<?>> {

    Class<E> eventClass();

    default EventPriority bukkitPriority() { return EventPriority.NORMAL; }

    Priority priority();

    Bounds localize(E event);

    boolean appliesTo(E event);

    @Override default int compareTo(RegionTrigger<?> other) {
        return this.priority().compareTo(other.priority());
    }
}
