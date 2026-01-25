package com.kntrel.mc.regionLib.region.repository;

import com.kntrel.mc.regionLib.region.Region;
import java.util.Collection;
import java.util.stream.StreamSupport;

public interface RegionRepository extends RegionReadRepository {

    //ABSTRACT
    void save(Region... region);


    //DEFAULT
    default void save(Iterable<Region> regions) {
        this.save(StreamSupport.stream(regions.spliterator(), false).toArray(Region[]::new));
    }
    default void delete(Region... regions) {
        for (Region region : regions) { region.destroy(); }
        this.save(regions);
    }
    default void delete(Collection<Region> regions) {
        this.delete(regions.toArray(new Region[0]));
    }
    default void deleteAll(Iterable<Region> regions) {
        regions.forEach(this::delete);
    }

}
