package com.kntrel.mc.regionLib.region.repository;

import com.kntrel.mc.regionLib.region.Region;
import java.util.Collection;
import java.util.stream.StreamSupport;

/**
 * Repository interface for persisting and deleting regions.
 */
public interface RegionRepository extends RegionReadRepository {

    //ABSTRACT
    /**
     * Persists the provided regions.
     *
     * @param region regions to save
     */
    void save(Region... region);


    //DEFAULT
    /**
     * Persists the provided regions.
     *
     * @param regions regions to save
     */
    default void save(Iterable<Region> regions) {
        this.save(StreamSupport.stream(regions.spliterator(), false).toArray(Region[]::new));
    }
    /**
     * Marks regions as destroyed and saves them.
     *
     * @param regions regions to delete
     */
    default void delete(Region... regions) {
        for (Region region : regions) { region.destroy(); }
        this.save(regions);
    }
    /**
     * Marks regions as destroyed and saves them.
     *
     * @param regions regions to delete
     */
    default void delete(Collection<Region> regions) {
        this.delete(regions.toArray(new Region[0]));
    }
    /**
     * Deletes each region in the iterable.
     *
     * @param regions regions to delete
     */
    default void deleteAll(Iterable<Region> regions) {
        regions.forEach(this::delete);
    }

}
