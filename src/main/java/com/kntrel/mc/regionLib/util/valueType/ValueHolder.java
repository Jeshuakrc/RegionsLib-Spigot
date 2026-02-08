package com.kntrel.mc.regionLib.util.valueType;

import org.jetbrains.annotations.NotNull;

public class ValueHolder<T> implements Comparable<ValueHolder<T>> {

    //API
    @SuppressWarnings("unchecked")
    public static <T> ValueHolder<T> of(@NotNull T value) {
        return new ValueHolder<>(value, ValueType.of((Class<T>) value.getClass()));
    }
    public static <T> ValueHolder<T> of(@NotNull String value, @NotNull ValueType<T> type) {
        return new ValueHolder<>(value, type);
    }


    //FIELDS
    private final ValueType<T> type_;
    private final String raw_;
    private final T val_;


    //CONSTRUCTORS
    protected ValueHolder(String value, ValueType<T> valueType) {
        this.type_ = valueType;
        this.raw_ = value;
        this.val_ = this.type_.valueOf(this.raw_);
    }
    protected ValueHolder(T value, ValueType<T> valueType) {
        this.type_ = valueType;
        this.val_ = value;
        this.raw_ = this.type_.toString(this.val_);
    }


    //UTILITY
    public T get() {
        return this.val_;
    }
    public ValueType<T> getType() {
        return type_;
    }


    //IMPLEMENTATION
    @Override public String toString() {
        return this.raw_;
    }
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (this.type_.getType().isInstance(o)) {
            T otherVal = this.type_.getType().cast(o);
            return otherVal.equals(this.val_);
        }
        if (o instanceof String s) { return s.equals(this.toString()); }
        if (o instanceof ValueHolder<?> other) {
            return other.getType().equals(this.getType()) && other.toString().equals(this.toString());
        }
        return false;
    }
    @Override public int hashCode() {
        return this.type_.hashCode() + this.raw_.hashCode();
    }
    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(@NotNull ValueHolder<T> other) {
        if (!Comparable.class.isAssignableFrom(this.type_.getType())) { return 0; }
        return ((Comparable<T>) this.val_).compareTo(other.val_);
    }
}
