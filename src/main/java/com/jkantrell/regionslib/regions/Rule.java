package com.jkantrell.regionslib.regions;

import com.google.gson.*;
import com.jkantrell.regionslib.RegionsLib;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class Rule {

    //FIELDS
    public final String name;
    private ValueHolder valueHolder_;

    private static final HashMap<String,Rule.DataType> keys_ = new HashMap<>();

    //CONSTRUCTORS
    public <T> Rule(String name,Rule.DataType<T> dataType, T value) {
        Rule.registerKey(name,dataType);
        this.valueHolder_ = new ValueHolder<T>(value);
        this.name = name;
    }

    //SETTERS
    public <T> boolean set(T value) {
        if (!valueHolder_.value_.getClass().isAssignableFrom(value.getClass())) { return false; }
        this.valueHolder_ = new ValueHolder<T>(value);
        return true;
    }

    //GETTERS
    public <T> T getValue(Rule.DataType<T> dataType) {
        if (!Rule.keys_.get(this.name).equals(dataType)) { return null; }
        return (T) valueHolder_.value_;
    }

    public DataType<?> getDatatype() {
        return keys_.get(name);
    }

    private static class ValueHolder <T> {
        private final T value_;

        private ValueHolder(T value) {
            this.value_ = value;
        }
    }

    //STATIC METHODS
    public static void registerKey(String name, Rule.DataType<?> dataType) {
        Logger logger = RegionsLib.getMain().getLogger();
        String type = dataType.getClazz().getSimpleName();
        if (keys_.containsKey(name)) {
            if (keys_.get(name).equals(dataType)) {
                logger.finer("Key '" + name + "' of type '" + type + "' already exists.");
                return;
            }
            String oldType = keys_.get(name).getClazz().getSimpleName();
            keys_.remove(name);
            keys_.put(name,dataType);
            logger.info("Rule key '" + name + "' already existed with type '" + oldType + "'. Replacing with type '" + type + "'.");
            return;
        }
        logger.fine("Registering new rule key of name '" + name + "' with type '" + type + "'.");
        Rule.keys_.put(name,dataType);
    }

    public static class DataType<T> {

        //CONSTANTS
        public static final Rule.DataType<String> STRING = new Rule.DataType<>(String.class, JsonPrimitive::new, JsonElement::getAsString);
        public static final Rule.DataType<Integer> INT = new Rule.DataType<>(Integer.class, JsonPrimitive::new, JsonElement::getAsInt);
        public static final Rule.DataType<Double> DOUBLE = new Rule.DataType<>(Double.class, JsonPrimitive::new, JsonElement::getAsDouble);
        public static final Rule.DataType<Boolean> BOOL = new Rule.DataType<>(Boolean.class, JsonPrimitive::new, JsonElement::getAsBoolean);
        public static final Rule.DataType<JsonElement> JSON = new DataType<>(JsonElement.class, j -> j, j -> j);

        //FIELDS
        private final Function<T,JsonElement> onSerialize_;
        private final Function<JsonElement,T> onDeserialize_;
        private final Class<T> type_;

        //CONSTRUCTOR
        public DataType(Class<T>type, Function<T,JsonElement> onSerialize, Function<JsonElement,T> onDeserialize){
            this.type_ = type;
            this.onSerialize_ = onSerialize;
            this.onDeserialize_ = onDeserialize;
        }

        //GETTERS
        public Class<T> getClazz() {
            return this.type_;
        }

        //METHODS
        private JsonElement serialize(T val) {
            return onSerialize_.apply(val);
        }
        private T deserialize(JsonElement json) {
            return onDeserialize_.apply(json);
        }

    }
    public static class EnumDataType<E extends Enum<E>> extends DataType<E> {

        public EnumDataType(Class<E> enumType) {
            super(enumType,e -> new JsonPrimitive(e.toString()), j -> E.valueOf(enumType, j.getAsString()));
        }
    }

    //SERIALIZATION CLASSES
    public static class JSerializer implements JsonSerializer<Rule> {

        @Override
        public JsonElement serialize(Rule src, Type typeOfSrc, JsonSerializationContext context) {

            JsonObject jsonRule = new JsonObject();
            try {
                jsonRule.add(src.name, Rule.keys_.get(src.name).serialize(src.valueHolder_.value_));
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
            String name = jsonRule.keySet().iterator().next();
            JsonElement jsonValueElm = jsonRule.get(name);
            Rule.DataType dataType;
            if (Rule.keys_.containsKey(name)) {
                dataType = Rule.keys_.get(name);
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
