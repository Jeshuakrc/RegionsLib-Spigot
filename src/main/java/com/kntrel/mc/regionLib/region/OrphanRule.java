package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Set;

final class OrphanRule implements Rule<String> {

    //FIELDS
    private final String name_;


    //CONSTRUCTOR
    public OrphanRule(@NotNull String name) {
        this.name_ = name;
    }


    //IMPLEMENTATION
    @Override public ValueType<String> valueType() {
        return ValueType.STRING;
    }
    @Override public String name() {
        return this.name_;
    }
    @Override public Collection<RegionTrigger<?>> triggers() {
        return Set.of();
    }
    @Override public void fire(String value, Event event, Region triggerer) {
        // Do nothing
    }
}
