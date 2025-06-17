package com.jkantrell.regionslib.region;

import com.jkantrell.regionslib.region.hierarchy.Hierarchy;
import com.jkantrell.regionslib.region.hierarchy.HierarchyRepository;
import com.jkantrell.regionslib.util.Area;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RegionRepository {
    List<Region> getAll();
    Optional<Region> get(Long id);
    Optional<Region> get(String name);
    List<Region> getAt(double x, double y, double z, World world);
    default List<Region> getAt(Location location) {
        return this.getAt(location.getX(), location.getY(), location.getZ(), location.getWorld());
    }
    List<Region> getIn(World world);
    List<Region> getIn(double x1, double y1, double z1, double x2, double y2, double z2, World world);
    default List<Region> getIn(Area area) {
        return this.getIn(area.getMinX(), area.getMinY(), area.getMinZ(), area.getMaxX(), area.getMaxY(), area.getMaxZ(), area.getWorld());
    }
    default List<Region> getIn(BoundingBox boundingBox, World world) {
        return this.getIn(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(), boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ(), world);
    }
    List<Region> getInChunk(int x, int z, World world);
    default List<Region> getInChunk(Chunk chunk) {
        return getInChunk(chunk.getX(), chunk.getZ(), chunk.getWorld());
    }
    void save(Region region);
    default void saveAll(Iterable<Region> regions) {
        regions.forEach(this::save);
    }
    HierarchyRepository getHierarchyRepository();
}
