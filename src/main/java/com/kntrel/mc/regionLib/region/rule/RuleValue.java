package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.util.valueType.ValueHolder;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.jetbrains.annotations.Nullable;

public class RuleValue<T> extends ValueHolder<T> {

    //FIELDS
    private final Rule<T> rule_;


    //CONSTRUCTOR
    public RuleValue(Rule<T> rule, String value) {
        super(value, rule.getValueType());
        this.rule_ = rule;
    }


    //GETTERS
    public Rule<T> getRule() {
        return this.rule_;
    }
}
