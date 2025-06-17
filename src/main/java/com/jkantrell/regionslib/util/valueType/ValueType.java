package com.jkantrell.regionslib.util.valueType;

import org.apache.commons.lang3.StringUtils;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class ValueType<T> {

    //CONSTANTS
    public static final ValueType<String> STRING = ValueType.create(String.class, Function.identity(), Function.identity());
    public static final ValueType<Integer> INT = ValueType.create(Integer.class,
            s -> { try { return Integer.valueOf(s); } catch (NumberFormatException e) { return 0; }}
    );
    public static final ValueType<Double> DOUBLE = ValueType.create(Double.class,
            s -> { try { return Double.valueOf(s); } catch (NumberFormatException e) { return .0; }}
    );
    public static final ValueType<Boolean> BOOL = ValueType.create(Boolean.class, s -> {
        String val = StringUtils.normalizeSpace(s);
        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("t")) { return true; }
        try { return Integer.parseInt(val) != 0; } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(val) != 0; } catch (NumberFormatException ignored) {}
        return false;
    });


    //ASSETS
    private static final Map<Class<?>, ValueType<?>> REGISTRY = new HashMap<>();


    //API
    @SuppressWarnings({"unchecked" , "rawtypes"})
    public static <T> ValueType<T> of(@Nonnull Class<T> clazz) {
        ValueType<?> r = REGISTRY.get(clazz);

        if (r == null && Enum.class.isAssignableFrom(clazz)) {
            Class<? extends Enum> enumRaw = (Class<? extends Enum>) clazz;
            r = ValueType.create(clazz,
                    s -> (T) Enum.valueOf(enumRaw, s),
                    e -> ((Enum<?>) e).name()
            );
        }

        if (r == null) {
            throw new NullPointerException("No registered ValueType with class '" + clazz.getName() + "'");
        }

        return (ValueType<T>) r;
    }
    public static <T> ValueType<T> create(Class<T> clazz, Function<String, T> deserializer, Function<T, String> serializer) {
        return new ValueType<T>(clazz) {
            @Override
            public T valueOf(String src) {
                return deserializer.apply(src);
            }

            @Override
            public String toString(T src) {
                return serializer.apply(src);
            }
        };
    }
    public static <T> ValueType<T> create(Class<T> clazz, Function<String, T> deserializer) {
        return ValueType.create(clazz, deserializer, Object::toString);
    }
    public static Map<Class<?>, ValueType<?>> getRegistry() {
        return Collections.unmodifiableMap(REGISTRY);
    }


    //FIELDS
    private final Class<T> clazz_;


    //CONSTRUCTORS
    public ValueType(Class<T> clazz) {
        if (REGISTRY.containsKey(clazz)) {
            throw new IllegalArgumentException("ValueType of class '" + clazz.getName() + "' is already defined");
        }
        this.clazz_ = clazz;
        REGISTRY.put(clazz, this);
    }


    //DEFINITION
    public final Class<T> getType() {
        return this.clazz_;
    }
    public abstract T valueOf(String src);
    public abstract String toString(T src);


    //IMPLEMENTATION
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (o instanceof Class<?> c) { return this.clazz_.equals(c); }
        if (!(o instanceof ValueType<?> other)) { return false; }
        return other.clazz_.equals(this.clazz_);
    }
    @Override public int hashCode() {
        return this.clazz_.hashCode();
    }
}
