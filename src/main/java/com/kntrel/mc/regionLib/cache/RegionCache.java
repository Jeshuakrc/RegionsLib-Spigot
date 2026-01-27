package com.kntrel.mc.regionLib.cache;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.util.cache.ConcurrentRLUCache;
import java.util.Optional;
import java.util.function.Consumer;

public class RegionCache {

    //FIELDS
    private final ConcurrentRLUCache<Long, RegionSnapshot> cache_;


    //CONSTRUCTORS
    public RegionCache(int capacity) {
        this.cache_ = new ConcurrentRLUCache<>(capacity);
    }
    public RegionCache() {
        this(1024);
    }


    //UTILITY
    public Optional<RegionSnapshot> get(long regionId) {
        return Optional.ofNullable(this.cache_.get(regionId));
    }
    public boolean isCached(long regionId) {
        return this.cache_.containsKey(regionId);
    }
    public void ifPresent(long regionId, Consumer<RegionSnapshot> action) {
        RegionSnapshot snapshot = this.cache_.get(regionId);
        if (snapshot != null) {
            action.accept(snapshot);
        }
    }
    public void put(RegionSnapshot snapshot) {
        this.cache_.put(snapshot.id(), snapshot);
    }
    public void put(Region region) {
        this.put(new RegionSnapshot(region));
    }
    public boolean evict(long regionId) {
        return this.cache_.remove(regionId) != null;
    }
    public long onEviction(Consumer<RegionSnapshot> callback) {
        return this.cache_.onEviction((key, value) -> callback.accept(value));
    }
    public void removeEvictionCallback(long id) {
        this.cache_.removeEvictionCallback(id);
    }
}