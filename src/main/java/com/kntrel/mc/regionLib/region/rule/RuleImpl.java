package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.event.Event;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;

public class RuleImpl<T> implements Rule<T> {

    //FIELDS
    private final String name_;
    private final ValueType<T> type_;
    private final Set<RegionTrigger<?>> triggers_;
    private final TriConsumer<T, Event, Region> action_;
    private final BiConsumer<Event, Region> absentAction_;


    //CONSTRUCTOR
    public RuleImpl(String name, ValueType<T> type, Collection<RegionTrigger<?>> triggers, TriConsumer<T, Event, Region> action, BiConsumer<Event, Region> absentAction) {
        this.name_ = name;
        this.type_ = type;
        this.triggers_ = Set.copyOf(triggers);
        this.action_ = action;
        this.absentAction_ = absentAction;
    }


    //IMPLEMENTATION
    @Override public String name() {
        return this.name_;
    }
    @Override public Set<RegionTrigger<?>> triggers() {
        return this.triggers_;
    }
    @Override public ValueType<T> valueType() {
        return this.type_;
    }
    @Override public void fire(T value, Event event, Region triggerer) {
        this.action_.accept(value, event, triggerer);
    }
    @Override public void fireOnAbsent(Event event, Region triggerer) {
        this.absentAction_.accept(event, triggerer);
    }
}
