package com.kntrel.mc.regionLib.region.repository;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Repository that allows saves to be attributed to an entity.
 */
public interface AttributedRegionRepository extends RegionRepository {

    //CONTRACT
    void save(@Nullable Entity doer, Region... region);


    //IMPLEMENTATION
    @Override default void save(Region... region) {
        this.save(null, region);
    }
}
