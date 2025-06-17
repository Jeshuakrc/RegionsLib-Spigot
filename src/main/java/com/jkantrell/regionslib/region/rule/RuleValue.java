package com.jkantrell.regionslib.region.rule;

import com.jkantrell.regionslib.util.valueType.ValueHolder;

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
