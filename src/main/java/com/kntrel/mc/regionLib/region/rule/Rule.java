package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import com.kntrel.mc.regionLib.region.listen.RegionListener;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.event.Event;

/**
 * Defines a region rule and its trigger behavior.
 *
 * @param <T> rule value type
 */
public interface Rule<T> extends RegionListener<RegionTrigger<?>> {

    //FACTORY
    /**
     * Creates a rule builder for a value type.
     *
     * @param type value type
     * @param <T> value type
     * @return rule builder
     */
    static <T> RuleBuilder.Starter<T> of(ValueType<T> type) {
        return RuleBuilder.of(type);
    }
    /**
     * Creates a rule builder for a class.
     *
     * @param clazz value class
     * @param <T> value type
     * @return rule builder
     */
    static <T> RuleBuilder.Starter<T> of(Class<T> clazz) {
        return RuleBuilder.of(clazz);
    }
    /**
     * Creates a boolean rule builder for an event.
     *
     * @param eventClass event class
     * @param <E> event type
     * @return rule builder
     */
    static <E extends Event> RuleBuilder.Bool<E> on(Class<E> eventClass) {
        return RuleBuilder.on(eventClass);
    }

    //CONTRACT
    /**
     * Returns the rule value type.
     *
     * @return value type
     */
    ValueType<T> valueType();
    /**
     * Fires the rule with a present value.
     *
     * @param value rule value
     * @param event triggering event
     * @param triggerer region triggering the rule
     */
    void fire(T value, Event event, Region triggerer);
    /**
     * Fires the rule when a value is absent.
     *
     * @param event triggering event
     * @param triggerer region triggering the rule
     */
    void fireOnAbsent(Event event, Region triggerer);
}
