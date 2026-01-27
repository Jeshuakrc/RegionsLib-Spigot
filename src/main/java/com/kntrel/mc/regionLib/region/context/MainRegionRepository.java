package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.cache.RegionCache;
import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import com.kntrel.mc.regionLib.event.RegionCreateEvent;
import com.kntrel.mc.regionLib.event.RegionUpdatedEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.repository.AttributedRegionRepository;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

class MainRegionRepository implements AttributedRegionRepository {

    //FIELDS
    private final RegionRepository saveDelegate_;
    private final RegionReadRepository readDelegate_;
    private final PluginManager pluginManager_;
    private final RegionCache cache_;


    //CONSTRUCTORS
    public MainRegionRepository(RegionRepository saveDelegate, RegionReadRepository readDelegate, PluginManager pluginManager, RegionCache cache) {
        this.saveDelegate_ = saveDelegate;
        this.readDelegate_ = readDelegate;
        this.pluginManager_ = pluginManager;
        this.cache_ = cache;
    }
    public MainRegionRepository(RegionRepository delegate, PluginManager pluginManager, RegionCache cache) {
        this(delegate, delegate, pluginManager, cache);
    }


    //IMPLEMENTATION
    @Override public List<Region> get(Query query) {
        List<Region> out = this.readDelegate_.get(query);
        out.forEach(this.cache_::put);
        return out;
    }
    @Override public void save(@Nullable Entity doer, Region... region) {
        Region[] inspected = Arrays.stream(region).filter(r -> this.inspect(doer, r)).toArray(Region[]::new);
        this.saveDelegate_.save(inspected);
        for (Region r : inspected) { this.cache_.put(r); }
    }


    //PRIVATE
    private boolean inspect(@Nullable Entity doer, Region region) {

        if (region.getId() == null) {
            RegionCreateEvent event = new RegionCreateEvent(region, doer);
            this.pluginManager_.callEvent(event);
            return !event.isCancelled();
        }

        RegionSnapshot old = this.cache_.get(region.getId()).orElse(null);
        if (old == null) {
            Region reg = this.readDelegate_.get(region.getId()).orElse(null);
            if (reg != null) {
                old = new RegionSnapshot(reg);
                this.cache_.put(old);
            }
        }
        if (old == null) { return true; }

        RegionSnapshot curr = new RegionSnapshot(region);
        RegionUpdatedEvent event = new RegionUpdatedEvent(region, old, curr, doer);
        this.pluginManager_.callEvent(event);

        return !event.isCancelled();
    }
}
