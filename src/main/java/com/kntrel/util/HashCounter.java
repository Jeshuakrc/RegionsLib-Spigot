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
    public int incrementAndGet(T key) {
        int count = this.getCount(key);
        this.setCount(key, ++count);
        return count;
    }
    public int getAndIncrement(T key) {
        int count = this.getCount(key);
        this.setCount(key, count + 1);
        return count;
    }
    public int decrementAndGet(T key) {
        int count = this.getCount(key);
        if (count == 0) { return 0; }
        this.setCount(key, --count);
        return count;
    }
    public int getAndDecrement(T key) {
        int count = this.getCount(key);
        if (count == 0) { return 0; }
        this.setCount(key, count - 1);
        return count;
    }
    public boolean contains(T key) {
        return this.counts_.containsKey(key);
    }
    public boolean isZero(T key) {
        return this.getCount(key) == 0;
    }
}
