package com.jkantrell.regionslib.regions;

import com.google.gson.*;
import com.jkantrell.regionslib.RegionsLib;
import java.lang.reflect.Type;
import java.util.*;

public class Rule {

    //FIELDS
    public final String name;
    private ValueHolder valueHolder_;
    private Rule.Key key_;

    private static final HashMap<String,Rule.Key> keys_ = new HashMap<>();

    //CONSTRUCTORS
    public <T> Rule(String name,Rule.DataType<T> dataType, T value) {
        this.setKey(name,dataType);
        this.valueHolder_ = new ValueHolder<T>(value);
        this.name = name;
    }

    //SETTERS
    public <T> boolean set(T value) {
        if (this.key_.dataType == null) { return false; }
        if (valueHolder_.value_.getClass().isAssignableFrom(value.getClass())) { return false; }
        this.valueHolder_ = new ValueHolder<T>(value);
        return true;
    }
    private void setKey(String name, Rule.DataType<?> dataType) {
        RegionsLib.getMain().getLogger().info("Processing rule key " + name + ". Type: " + dataType.getClass().toString());

        if (keys_.containsKey(name)) {
            if (keys_.get(name).dataType.equals(dataType)) {
                RegionsLib.getMain().getLogger().info("Key already exists.");
                this.key_ = keys_.get(name);
                return;
            }
            throw new IllegalArgumentException("Rule key already exists and DataType is not the same provided");
        }
        RegionsLib.getMain().getLogger().info("Key doesn't exist. Adding it");
        this.key_ = new Rule.Key(name,dataType);
    }

    //GETTERS
    public <T> T getValue(Rule.DataType<T> dataType) {
        if (!this.key_.dataType.equals(dataType)) { return null; }
        return (T) valueHolder_.value_;
    }
    public Rule.Key getKey() {
        return this.key_;
    }

    private static class ValueHolder <T> {
        private final T value_;

        private ValueHolder(T value) {
            this.value_ = value;
        }
    }

    public static class Key {

        public final String name;
        public final Rule.DataType dataType;

        public Key(String name, Rule.DataType<?> dataType) {
            this.name = name;
            this.dataType = dataType;
            if (Rule.keys_.containsKey(name)) { throw new IllegalArgumentException("Key with tha name " + name + " already exists"); }
            Rule.keys_.putIfAbsent(name,this);
        }

    }
    public static class DataType<T> {

        //CONSTANTS
        public static final Rule.DataType<String> STRING = new Rule.DataType<>(JsonPrimitive::new, JsonElement::getAsString);
        public static final Rule.DataType<Integer> INT = new Rule.DataType<>(JsonPrimitive::new, JsonElement::getAsInt);
        public static final Rule.DataType<Double> DOUBLE = new Rule.DataType<>(JsonPrimitive::new, JsonElement::getAsDouble);
        public static final Rule.DataType<Boolean> BOOL = new Rule.DataType<>(JsonPrimitive::new, JsonElement::getAsBoolean);
        public static final Rule.DataType<JsonElement> JSON = new DataType<>(j -> j, j -> j);

        //FIELDS
        private final OnSerialize<T> onSerialize_;
        private final OnDeserialize<T> onDeserialize_;

        //CONSTRUCTOR
        public DataType(OnSerialize<T> onSerialize, OnDeserialize<T> onDeserialize){
            this.onSerialize_ = onSerialize;
            this.onDeserialize_ = onDeserialize;
        }
        //METHODS
        private JsonElement serialize(T val) {
            return onSerialize_.serialize(val);
        }
        private T deserialize(JsonElement json) {
            return onDeserialize_.deserialize(json);
        }

        //INTERFACES
        @FunctionalInterface
        public interface OnSerialize<T> {
            JsonElement serialize(T val);
        }
        @FunctionalInterface
        public interface OnDeserialize<T> {
            T deserialize(JsonElement json);
        }
    }
    public static class EnumDataType<E extends Enum<E>> extends DataType<E> {

        public EnumDataType(Class<E> enumType) {
            super(e -> new JsonPrimitive(e.toString()), j -> E.valueOf(enumType, j.getAsString()));
        }
    }

    //SERIALIZATION CLASSES
    public static class JSerializer implements JsonSerializer<Rule> {

        @Override
        public JsonElement serialize(Rule src, Type typeOfSrc, JsonSerializationContext context) {

            JsonObject jsonRule = new JsonObject();
            try {
                jsonRule.add(src.name, src.key_.dataType.serialize(src.valueHolder_.value_));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            jsonRule.addProperty("rule","xd");
            return jsonRule;
        }
    }

    public static class JDeserializer implements JsonDeserializer<Rule> {

        @Override
        public Rule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonRule = json.getAsJsonObject();
            String name = jsonRule.keySet().iterator().next();
            JsonElement jsonValueElm = jsonRule.get(name);
            Rule.DataType dataType;
            if (Rule.keys_.containsKey(name)) {
                dataType = Rule.keys_.get(name).dataType;
            } else {
                if (jsonValueElm.isJsonPrimitive()) {
                    JsonPrimitive jsonValue = jsonValueElm.getAsJsonPrimitive();
                    if (jsonValue.isNumber()) {
                        Number number = jsonValue.getAsNumber();
                        if (number.doubleValue() % 1 == 0) {
                            dataType = Rule.DataType.INT;
                        } else {
                            dataType = Rule.DataType.DOUBLE;
                        }
                    } else if (jsonValue.isBoolean()) {
                        dataType = Rule.DataType.BOOL;
                    } else {
                        dataType = Rule.DataType.STRING;
                    }
                } else {
                    dataType = Rule.DataType.JSON;
                }
            }
            return new Rule(name,dataType,dataType.deserialize(jsonValueElm));
        }
    }
}
