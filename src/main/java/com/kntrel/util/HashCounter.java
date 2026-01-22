package com.kntrel.util;

import java.util.HashMap;
import java.util.Map;

public class HashCounter<T> {

    //FIELDS
    private final Map<T, Integer> counts_;


    //CONSTRUCTORS
    public HashCounter() {
        this.counts_ = new HashMap<>();
    }


    //API
    private int getCount(T key) {
        return this.counts_.getOrDefault(key, 0);
    }
    private void setCount(T key, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative. Provided: " + count);
        }
        if (count == 0) {
            this.counts_.remove(key);
            return;
        }
        this.counts_.put(key, count);
    }
    public boolean clear(T key) {
        return this.counts_.remove(key) != null;
    }
    public void incrementBy(T key, int amount) {
        this.counts_.compute(key, (k, v) -> {
            int newCount = (v == null ? 0 : v) + amount;
            return (newCount < 1) ? null : newCount;
        });
    }
    public void decrementBy(T key, int amount) {
        this.incrementBy(key, -amount);
    }
    public void increment(T key) {
        this.incrementBy(key, 1);
    }
    public void decrement(T key) {
        this.decrementBy(key, 1);
    }
    public int incrementByAndGet(T key, int amount) {
        this.incrementBy(key, amount);
        return this.getCount(key);
    }
    public int decrementByAndGet(T key, int amount) {
        this.decrementBy(key, amount);
        return this.getCount(key);
    }
    public int getAndIncrementBy(T key, int amount) {
        int current = this.getCount(key);
        this.incrementBy(key, amount);
        return current;
    }
    public int getAndDecrementBy(T key, int amount) {
        int current = this.getCount(key);
        this.decrementBy(key, amount);
        return current;
    }
    public int incrementAndGet(T key) {
        return this.incrementByAndGet(key, 1);
    }
    public int decrementAndGet(T key) {
        return this.decrementByAndGet(key, 1);
    }
    public int getAndIncrement(T key) {
        return this.getAndIncrementBy(key, 1);
    }
    public int getAndDecrement(T key) {
        return this.getAndDecrementBy(key, 1);
    }
    public boolean decrementByAndCheckZero(T key, int amount) {
        this.decrementBy(key, amount);
        return this.isZero(key);
    }
    public boolean decrementAndCheckZero(T key) {
        return this.decrementByAndCheckZero(key, 1);
    }
    public boolean decrementByAndCheckEmptied(T key, int amount) {
        int before = this.getAndDecrementBy(key, amount);
        return before > 0 && this.isZero(key);
    }
    public boolean decrementAndCheckEmptied(T key) {
        return this.decrementByAndCheckEmptied(key, 1);
    }
    public boolean contains(T key) {
        return this.counts_.containsKey(key);
    }
    public boolean isZero(T key) {
        return this.getCount(key) == 0;
    }
}
