package com.jkantrell.regionslib.regions.abilities;

import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import java.util.ArrayList;
import java.util.List;

class AbilityListenerList {
    private static class Entry<E extends Event> {
        private final Class<E> eventClass;
        private final EventPriority priority;
        private final AbilityListener<?> listener;

        private Entry(Class<E> eventClass, EventPriority priority, AbilityListener<E> listener) {
            this.eventClass = eventClass;
            this.priority = priority;
            this.listener = listener;
        }
    }

    private final ArrayList<AbilityListenerList.Entry<?>> entries_ = new ArrayList<>();

    //ADD METHODS
    <E extends Event> void add(Class<E> eventClass, AbilityListener<E> listener) {
        Entry<E> entry = new Entry<>(eventClass,listener.priority,listener);
        entries_.add(entry);
    }

    //CONTAINS METHODS
    boolean contains(Class<? extends Event> eventClass, EventPriority priority) {
        return (this.get(eventClass,priority) != null);
    }

    //GET METHODS
    AbilityListener<? extends Event> get(Class<? extends Event> eventClass, EventPriority priority) {
        for (Entry<?> entry : entries_) {
            if (entry.eventClass.equals(eventClass) && entry.priority.equals(priority)) {
                return entry.listener;
            }
        }
        return null;
    }

    //LIST METHODS
    List<AbilityListener<?>> toList() {
        ArrayList<AbilityListener<?>> list = new ArrayList<>();
        for (Entry entry : entries_) {
            list.add(entry.listener);
        }
        return list;
    }
}
