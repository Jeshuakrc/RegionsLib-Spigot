package com.jkantrell.regionslib.regions.rules;

import com.google.gson.JsonPrimitive;

public class RuleEnumDataType<E extends Enum<E>> extends RuleDataType<E> {

    public RuleEnumDataType(Class<E> enumType) {
        super(enumType, e -> new JsonPrimitive(e.toString()), j -> E.valueOf(enumType, j.getAsString()));
    }
}
