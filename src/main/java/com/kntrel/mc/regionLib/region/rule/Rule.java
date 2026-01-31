package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.trigger.RegionTrigger;
import com.kntrel.mc.regionLib.trigger.RegionListener;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.event.Event;

public interface Rule<T> extends RegionListener<RegionTrigger<?>> {

    //FACTORY
    static <T> RuleBuilder.Starter<T> of(ValueType<T> type) {
        return RuleBuilder.of(type);
    }
    static <T> RuleBuilder.Starter<T> of(Class<T> clazz) {
        return RuleBuilder.of(clazz);
    }
    static <E extends Event> RuleBuilder.Bool<E> on(Class<E> eventClass) {
        return RuleBuilder.on(eventClass);
    }

    //CONTRACT
    ValueType<T> valueType();
    void fire(T value, Event event, Region triggerer);
    void fireOnAbsent(Event event, Region triggerer);
}
