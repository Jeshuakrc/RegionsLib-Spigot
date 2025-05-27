package com.jkantrell.regionslib.region.rule;

import com.google.gson.*;
import com.jkantrell.regionslib.RegionsLib;
import java.lang.reflect.Type;

public record Rule(String key, String val) {

    public <T> T retrieve(RuleDataType<T> dataType) {
        return dataType.deserialize(val);
    }

}