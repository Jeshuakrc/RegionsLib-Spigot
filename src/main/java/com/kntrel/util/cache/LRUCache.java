package com.kntrel.util.cache;

import java.util.*;
import java.util.function.BiConsumer;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    //FIELDS
    private final int capacity_;
    private final Map<Long, BiConsumer<K, V>> evictionCallbacks_;
    private long callbackIdCounter_;


    //CONSTRUCTORS
    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity_ = capacity;
        this.evictionCallbacks_ = new HashMap<>();
        this.callbackIdCounter_ = 0L;
    }


    //GETTERS
    public int capacity() {
        return this.capacity_;
    }


    //UTILITY
    public long onEviction(BiConsumer<K, V> callback) {
        long id = this.callbackIdCounter_++;
        this.evictionCallbacks_.put(id, callback);
        return id;
    }
    public void removeEvictionCallback(long id) {
        this.evictionCallbacks_.remove(id);
    }


    //IMPLEMENTATION
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (this.size() <= this.capacity_) {
            return false;
        }
        for (BiConsumer<K, V> callback : this.evictionCallbacks_.values()) {
            callback.accept(eldest.getKey(), eldest.getValue());
        }
        return true;
    }

}
