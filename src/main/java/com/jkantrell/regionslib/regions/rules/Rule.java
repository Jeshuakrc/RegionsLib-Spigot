package com.jkantrell.regionslib.regions.rules;

import com.google.gson.*;
import com.jkantrell.regionslib.RegionsLib;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;

public class Rule {

    //FIELDS
    private final RuleKey key_;
    private ValueHolder valueHolder_;

    //CONSTRUCTORS
    public <T> Rule(String label, RuleDataType<T> dataType, T value) {
        this.valueHolder_ = new ValueHolder<T>(value);
        RuleKey key = RuleKey.get(label);
        if (key == null) {
            RegionsLib.getMain().getLogger().fine(
            "Registering rule '" + label + "'. No rule key under this label. Proactively registering new key under a '" + dataType.getClazz().getSimpleName() + "' data type."
            );
            this.key_ = RuleKey.registerNew(RegionsLib.getMain(),label,dataType);
            this.key_.setDisposable(true);
        } else if (key.getDataType().equals(dataType)) {
            this.key_ = key;
        } else {
            throw new IllegalArgumentException(
                "The label '" + label + "' has already been registered for '" + key.getDataType().getClazz().getSimpleName() + "' data type. you're passing a '" + dataType.getClazz().getSimpleName() + "' type."
            );
        }
    }
    public Rule(RuleKey key, Object value) {
        this.key_ = key;
        this.set(value);
    }

    //SETTERS
    public <T> void set(T value) {
        RuleKey key = this.key_;
        if (!key.getDataType().getClazz().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(
                "Rule key is if type '" + key.getDataType().getClazz().getSimpleName() + "' but provided value is '" + value.getClass().getSimpleName() + "'."
            );
        }
        this.valueHolder_ = new ValueHolder<T>(value);
    }

    //GETTERS
    public Object getValue() {
        return valueHolder_.value_;
    }
    public <T> T getValue(RuleDataType<T> dataType) {
        if (!this.getDatatype().equals(dataType)) { return null; }
        return (T) valueHolder_.value_;
    }
    public RuleDataType getDatatype() {
        return this.key_.getDataType();
    }
    public String getLabel() {
        return this.key_.getLabel();
    }

    private static class ValueHolder <T> {
        private final T value_;

        private ValueHolder(T value) {
            this.value_ = value;
        }
    }

    //SERIALIZATION CLASSES
    public static class JSerializer implements JsonSerializer<Rule> {

        @Override
        public JsonElement serialize(Rule src, Type typeOfSrc, JsonSerializationContext context) {

            JsonObject jsonRule = new JsonObject();
            try {
                jsonRule.add(src.getLabel(), src.getDatatype().serialize(src.valueHolder_.value_));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return jsonRule;
        }
    }

    public static class JDeserializer implements JsonDeserializer<Rule> {

        @Override
        public Rule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonRule = json.getAsJsonObject();
            String label = jsonRule.keySet().iterator().next();
            JsonElement jsonValueElm = jsonRule.get(label);
            RuleDataType dataType;

            if (RuleKey.exists(label)) {
                dataType = RuleKey.get(label).getDataType();
            } else {
                if (jsonValueElm.isJsonPrimitive()) {
                    JsonPrimitive jsonValue = jsonValueElm.getAsJsonPrimitive();
                    if (jsonValue.isNumber()) {
                        Number number = jsonValue.getAsNumber();
                        if (number.doubleValue() % 1 == 0) {
                            dataType = RuleDataType.INT;
                        } else {
                            dataType = RuleDataType.DOUBLE;
                        }
                    } else if (jsonValue.isBoolean()) {
                        dataType = RuleDataType.BOOL;
                    } else {
                        dataType = RuleDataType.STRING;
                    }
                } else {
                    dataType = RuleDataType.JSON;
                }
            }
            return new Rule(label,dataType, dataType.deserialize(jsonValueElm));
        }
    }
}
