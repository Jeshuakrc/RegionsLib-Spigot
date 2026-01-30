package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.trigger.TriggerListener;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.event.Event;

import java.util.Collection;
import java.util.Set;

public class Rule<T> implements TriggerListener<RuleTrigger<T, ?>> {

    //FACTORY
    public static <T> RuleBuilder.Starter<T> of(ValueType<T> type) {
        return RuleBuilder.of(type);
    }
    public static <T> RuleBuilder.Starter<T> of(Class<T> clazz) {
        return RuleBuilder.of(clazz);
    }
    public static <E extends Event> RuleBuilder.Bool<E> on(Class<E> eventClass) {
        return RuleBuilder.on(eventClass);
    }


    //FIELDS
    private final String name_;
    private final Set<RuleTrigger<T, ?>> triggers_;
    private final ValueType<T> type_;


    //CONSTRUCTOR
    public Rule(String name, Collection<RuleTrigger<T, ?>> triggers, ValueType<T> type) {
        this.name_ = name;
        this.triggers_ = Set.copyOf(triggers);
        this.type_ = type;
    }


    //IMPLEMENTATION
    @Override public String name() {
        return this.name_;
    }
    @Override public Set<RuleTrigger<T, ?>> triggers() {
        return this.triggers_;
    }
    public ValueType<T> getValueType() {
        return this.type_;
    }
}
