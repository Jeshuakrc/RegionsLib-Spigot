package com.kntrel.mc.regionLib.region.repository;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public interface RegionRepository {

    //ABSTRACT
    List<Region> get(Query query);
    void save(Region... region);
    HierarchyRepository getHierarchyRepository();


    //DEFAULT METHODS
    default List<Region> get(Condition condition) {
        return get(new Query(condition, false));
    }
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
    default List<Region> getAll() {
        return this.get(Condition.TRUE);
    }
    default Optional<Region> get(Long id) {
        List<Region> regions = this.get(new Query(Condition.equal(RegionField.ID, id), 1, false));
        if (regions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(regions.getFirst());
    }
    default List<Region> get(String name) {
        return this.get(Condition.equal(RegionField.NAME, name));
    }
    default FluidRegionRepository.QueryBuilder where() {
        return new FluidRegionRepository.QueryBuilder(this);
    }
    default FluidRegionRepository.QueryBuilder where(Condition... conditions) {
        return new FluidRegionRepository.QueryBuilder(this).and(conditions);
    }
    default List<Region> getAt(Location point) {
        return this.get(Condition.at(point));
    }
    default List<Region> getAt(double x, double y, double z, World world) {
        return this.get(Condition.at(x, y, z, world));
    }
    default List<Region> getIn(Area area) {
        return this.get(Condition.in(area));
    }
    default List<Region> getIn(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        return this.get(Condition.in(x1, y1, z1, x2, y2, z2, world));
    }
    default List<Region> getIn(World world) {
        return this.get(Condition.in(world));
    }
    default List<Region> getIn(BoundingBox boundingBox, World world) {
        return this.get(Condition.in(boundingBox, world));
    }
    default List<Region> getIn(Region other) {
        return this.get(Condition.in(other));
    }
    default List<Region> inChunk(int x, int z, World world) {
        return this.get(Condition.inChunk(x, z, world));
    }
    default List<Region> inChunk(Chunk chunk) {
        return this.get(Condition.inChunk(chunk));
    }
}
