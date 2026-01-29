package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.trigger.TriggerListener;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import java.util.Collection;
import java.util.Set;

public class RuleN<T> implements TriggerListener<RuleTrigger<T, ?>> {

    //FIELDS
    private final String name_;
    private final Set<RuleTrigger<T, ?>> triggers_;
    private final ValueType<T> type_;


    //CONSTRUCTOR
    public RuleN(String name, Collection<RuleTrigger<T, ?>> triggers, ValueType<T> type) {
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
}
