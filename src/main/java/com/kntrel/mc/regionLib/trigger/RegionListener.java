package com.kntrel.mc.regionLib.trigger;

import org.bukkit.event.Event;

import java.util.Collection;

public interface RegionListener<T extends RegionTrigger<?>> extends TriggerTarget<Event, T> {

    String name();

    Collection<T> triggers();
}