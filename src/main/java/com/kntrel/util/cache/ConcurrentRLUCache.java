package com.kntrel.util.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentRLUCache<K, V> implements ConcurrentMap<K, V> {

    //FIELDS
    private final LRUCache<K, V> delegate_;
    private final Object lock_;


    //CONSTRUCTORS
    public ConcurrentRLUCache(int capacity) {
        this.delegate_ = new LRUCache<>(capacity);
        this.lock_ = new Object();
    }


    //GETTERS
    public int capacity() {
        return this.delegate_.capacity();
    }


    //IMPLEMENTATION
    @Override
    public int size() { synchronized (this.lock_) {
        return delegate_.size();
    }}
    @Override
    public boolean isEmpty() { synchronized (this.lock_) {
        return delegate_.isEmpty();
    }}
    @Override
    public boolean containsKey(Object key) { synchronized (this.lock_) {
        return delegate_.containsKey(key);
    }}
    @Override
    public boolean containsValue(Object value) { synchronized (this.lock_) {
        return delegate_.containsValue(value);
    }}
    @Override
    public V get(Object key) { synchronized (this.lock_) {
        return delegate_.get(key);
    }}
    @Nullable
    @Override
    public V put(K key, V value) { synchronized (this.lock_) {
        return delegate_.put(key, value);
    }}
    @Override
    public V remove(Object key) { synchronized (this.lock_) {
        return delegate_.remove(key);
    }}
    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) { synchronized (this.lock_) {
        delegate_.putAll(m);
    }}
    @Override
    public void clear() { synchronized (this.lock_) {
        delegate_.clear();
    }}
    @NotNull
    @Override
    public Set<K> keySet() { synchronized (this.lock_) {
        return delegate_.keySet();
    }}
    @NotNull
    @Override
    public Collection<V> values() { synchronized (this.lock_) {
        return delegate_.values();
    }}
    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() { synchronized (this.lock_) {
        return delegate_.entrySet();
    }}
    @Override
    public V putIfAbsent(@NotNull K key, V value) { synchronized (this.lock_) {
        if (!delegate_.containsKey(key)) {
            return delegate_.put(key, value);
        }
        return delegate_.get(key);
    }}
    @Override
    public boolean remove(@NotNull Object key, Object value) { synchronized (this.lock_) {
        if (delegate_.containsKey(key) && delegate_.get(key).equals(value)) {
            delegate_.remove(key);
            return true;
        }
        return false;
    }}
    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) { synchronized (this.lock_) {
        if (delegate_.containsKey(key) && delegate_.get(key).equals(oldValue)) {
            delegate_.put(key, newValue);
            return true;
        }
        return false;
    }}
    @Override
    public V replace(@NotNull K key, @NotNull V value) { synchronized (this.lock_) {
        if (delegate_.containsKey(key)) {
            return delegate_.put(key, value);
        }
        return null;
    }}
}
