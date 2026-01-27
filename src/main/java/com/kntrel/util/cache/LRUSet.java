package com.kntrel.util.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class LRUSet<T> implements Set<T> {

    //FIELDS
    private final LinkedHashSet<T> inner_;
    private final int capacity_;


    //CONSTRUCTOR
    public LRUSet(int capacity) {
        this.inner_ = new LinkedHashSet<>();
        this.capacity_ = capacity;
    }


    //IMPLEMENTATION
    @Override public int size() {
        return inner_.size();
    }

    @Override public boolean isEmpty() {
        return inner_.isEmpty();
    }

    @Override public boolean contains(Object o) {
        return inner_.contains(o);
    }

    @NotNull
    @Override public Iterator<T> iterator() {
        return inner_.iterator();
    }

    @NotNull
    @Override public Object[] toArray() {
        return inner_.toArray();
    }

    @NotNull
    @Override public <T1> T1[] toArray(@NotNull T1[] a) {
        return inner_.toArray(a);
    }

    @Override public boolean add(T t) {
        // If element already exists, remove it to update position
        boolean isNew = !inner_.remove(t);
        // Add to end (LinkedHashSet maintains insertion order)
        inner_.add(t);
        // If exceeded capacity, remove the least recently used (oldest) element
        if (inner_.size() > capacity_) { this.inner_.removeFirst(); }
        return isNew;
    }

    @Override public boolean remove(Object o) {
        return inner_.remove(o);
    }

    @Override public boolean containsAll(@NotNull Collection<?> c) {
        return inner_.containsAll(c);
    }

    @Override public boolean addAll(@NotNull Collection<? extends T> c) {
        boolean changed = false;
        for (T element : c) {
            if (add(element)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override public boolean retainAll(@NotNull Collection<?> c) {
        return inner_.retainAll(c);
    }

    @Override public boolean removeAll(@NotNull Collection<?> c) {
        return inner_.removeAll(c);
    }

    @Override public void clear() {
        inner_.clear();
    }
}
