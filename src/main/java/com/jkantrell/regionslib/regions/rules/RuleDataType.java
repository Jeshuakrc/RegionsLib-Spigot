package com.jkantrell.regionslib.regions.rules;

import com.google.gson.*;

import java.util.function.Function;

public class RuleDataType<T> {

    //CONSTANTS
    public static final RuleDataType<String> STRING = new RuleDataType<>(String.class, JsonPrimitive::new, JsonElement::getAsString);
    public static final RuleDataType<Integer> INT = new RuleDataType<>(Integer.class, JsonPrimitive::new, JsonElement::getAsInt);
    public static final RuleDataType<Double> DOUBLE = new RuleDataType<>(Double.class, JsonPrimitive::new, JsonElement::getAsDouble);
    public static final RuleDataType<Boolean> BOOL = new RuleDataType<>(Boolean.class, JsonPrimitive::new, JsonElement::getAsBoolean);
    public static final RuleDataType<JsonElement> JSON = new RuleDataType<>(JsonElement.class, j -> j, j -> j);

    //FIELDS
    private final Function<T, JsonElement> onSerialize_;
    private final Function<JsonElement, T> onDeserialize_;
    private final Class<T> type_;

    //CONSTRUCTOR
    public RuleDataType(Class<T> type, Function<T, JsonElement> onSerialize, Function<JsonElement, T> onDeserialize) {
        this.type_ = type;
        this.onSerialize_ = onSerialize;
        this.onDeserialize_ = onDeserialize;
    }

    //GETTERS
    public Class<T> getClazz() {
        return this.type_;
    }

    //METHODS
    JsonElement serialize(T val) {
        return onSerialize_.apply(val);
    }
    T deserialize(JsonElement json) {
        return onDeserialize_.apply(json);
    }

    public T deserialize (String src) {
        return this.deserialize(new JsonParser().parse(src));
    }
    public String serializeToString(T val) {
        return this.serialize(val).toString();
    }

}
