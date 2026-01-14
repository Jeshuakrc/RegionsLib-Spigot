package com.kntrel.util.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity_;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity_ = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return this.size() > this.capacity_;
    }

    public int capacity() {
        return this.capacity_;
    }
}
