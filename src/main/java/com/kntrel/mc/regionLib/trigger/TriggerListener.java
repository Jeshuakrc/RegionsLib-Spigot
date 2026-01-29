package com.kntrel.mc.regionLib.trigger;

import java.util.Collection;

public interface TriggerListener<T extends RegionTrigger<?>> {

    String name();

    Collection<T> triggers();
}
