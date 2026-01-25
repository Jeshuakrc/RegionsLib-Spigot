package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.event.RegionCreateEvent;
import com.kntrel.mc.regionLib.event.RegionDestroyEvent;
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


    //CONSTRUCTORS
    public MainRegionRepository(RegionRepository saveDelegate, RegionReadRepository readDelegate, PluginManager pluginManager) {
        this.saveDelegate_ = saveDelegate;
        this.readDelegate_ = readDelegate;
        this.pluginManager_ = pluginManager;
    }
    public MainRegionRepository(RegionRepository delegate, PluginManager pluginManager) {
        this(delegate, delegate, pluginManager);
    }


    //IMPLEMENTATION
    @Override public List<Region> get(Query query) {
        return this.readDelegate_.get(query);
    }
    @Override public void save(@Nullable Entity doer, Region... region) {
        this.saveDelegate_.save(
                Arrays.stream(region).filter(r -> this.inspect(doer, r)).toArray(Region[]::new)
        );
    }


    //PRIVATE
    private boolean inspect(@Nullable Entity doer, Region region) {

        if (region.getId() == null) {
            RegionCreateEvent createEvent = new RegionCreateEvent(region, doer);
            this.pluginManager_.callEvent(createEvent);
            if (createEvent.isCancelled()) { return false; }
        }

        if (region.isDestroyed()) {
            RegionDestroyEvent destroyEvent = new RegionDestroyEvent(region, doer);
            this.pluginManager_.callEvent(destroyEvent);
            if (destroyEvent.isCancelled()) { return false; }
        }

        return true;
    }
}
