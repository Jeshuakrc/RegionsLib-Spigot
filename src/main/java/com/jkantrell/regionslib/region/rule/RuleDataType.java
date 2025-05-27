package com.jkantrell.regionslib.region.rule;

import org.apache.commons.lang3.StringUtils;
import java.util.function.Function;

public class RuleDataType<T> {

    //STATIC
    public static <E extends java.lang.Enum<E>> RuleDataType<E> ofEnum(Class<E> eClass) {
        return new RuleDataType.Enum<E>(eClass);
    }


    //CONSTANTS
    public static final RuleDataType<String> STRING = new RuleDataType<>(String.class, Function.identity());
    public static final RuleDataType<Integer> INT = new RuleDataType<>(Integer.class,
        s -> { try { return Integer.valueOf(s); } catch (NumberFormatException e) { return 0; }}
    );
    public static final RuleDataType<Double> DOUBLE = new RuleDataType<>(Double.class,
        s -> { try { return Double.valueOf(s); } catch (NumberFormatException e) { return .0; }}
    );
    public static final RuleDataType<Boolean> BOOL = new RuleDataType<>(Boolean.class, s -> {
        String val = StringUtils.normalizeSpace(s);
        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("t")) { return true; }
        try { return Integer.parseInt(val) != 0; } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(val) != 0; } catch (NumberFormatException ignored) {}
        return false;
    });


    //FIELDS
    private final Function<T, String> serializer_;
    private final Function<String, T> deserializer_;
    private final Class<T> type_;


    //CONSTRUCTOR
    public RuleDataType(Class<T> type, Function<T, String> serializer, Function<String, T> deserializer) {
        this.type_ = type;
        this.serializer_ = serializer;
        this.deserializer_ = deserializer;
    }
    public RuleDataType(Class<T> type, Function<String, T> deserializer) {
        this(type, Object::toString, deserializer);
    }


    //GETTERS
    public Class<T> getClazz() {
        return this.type_;
    }


    //METHODS
    public String serialize(T val) {
        return serializer_.apply(val);
    }
    public T deserialize(String src) {
        return deserializer_.apply(src);
    }


    //CLASSES
    public static class Enum<E extends java.lang.Enum<E>> extends RuleDataType<E> {
        private Enum(Class<E> enumType) {
            super(enumType, s -> E.valueOf(enumType, s));
        }
    }
}
