package com.kntrel.mc.regionLib.region.repository;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.util.tuple.Pair;
import com.kntrel.util.tuple.Triplet;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import java.util.List;
import java.util.Optional;

/**
 * Read-only repository for querying regions.
 */
public interface RegionReadRepository {

    //CONTRACT
    /**
     * Executes a query for regions.
     *
     * @param query query definition
     * @return list of matching regions
     */
    List<Region> get(Query query);
    /**
     * Executes a projected query for one or more region fields.
     *
     * @param query query definition
     * @param fields projected fields to return in order
     * @return ordered projected rows
     */
    default List<Object[]> get(Query query, RegionField<?>... fields) {
        if (fields.length < 1) {
            throw new IllegalArgumentException("Projected region queries must request at least one field.");
        }
        return this.get(query).stream()
                .map(region -> {
                    Object[] row = new Object[fields.length];
                    for (int i = 0; i < fields.length; i++) {
                        row[i] = fields[i].extract(region);
                    }
                    return row;
                })
                .toList();
    }
    /**
     * Executes a projected query for a single region field.
     *
     * @param query query definition
     * @param field projected field
     * @return field values
     * @param <T> field type
     */
    @SuppressWarnings("unchecked")
    default <T> List<T> get(Query query, RegionField<T> field) {
        return this.get(query, new RegionField<?>[] { field }).stream()
                .map(row -> (T) row[0])
                .toList();
    }
    /**
     * Executes a projected query for two region fields.
     *
     * @param query query definition
     * @param first first projected field
     * @param second second projected field
     * @return projected pairs
     * @param <A> first field type
     * @param <B> second field type
     */
    @SuppressWarnings("unchecked")
    default <A, B> List<Pair<A, B>> get(Query query, RegionField<A> first, RegionField<B> second) {
        return this.get(query, new RegionField<?>[] { first, second }).stream()
                .map(row -> Pair.of((A) row[0], (B) row[1]))
                .toList();
    }
    /**
     * Executes a projected query for three region fields.
     *
     * @param query query definition
     * @param first first projected field
     * @param second second projected field
     * @param third third projected field
     * @return projected triplets
     * @param <A> first field type
     * @param <B> second field type
     * @param <C> third field type
     */
    @SuppressWarnings("unchecked")
    default <A, B, C> List<Triplet<A, B, C>> get(Query query, RegionField<A> first, RegionField<B> second, RegionField<C> third) {
        return this.get(query, new RegionField<?>[] { first, second, third }).stream()
                .map(row -> Triplet.of((A) row[0], (B) row[1], (C) row[2]))
                .toList();
    }


    //DEFAULT
    /**
     * Queries regions with a condition.
     *
     * @param condition query condition
     * @return list of matching regions
     */
    default List<Region> get(Condition condition) {
        return get(new Query(condition, false));
    }
    /**
     * Returns all regions.
     *
     * @return list of regions
     */
    default List<Region> getAll() {
        return this.get(Condition.TRUE);
    }
    /**
     * Returns a region by id.
     *
     * @param id region id
     * @return optional region
     */
    default Optional<Region> get(Long id) {
        List<Region> regions = this.get(new Query(Condition.equal(RegionField.ID, id), 1, false));
        if (regions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(regions.getFirst());
    }
    /**
     * Returns regions matching a name.
     *
     * @param name region name
     * @return list of matching regions
     */
    default List<Region> get(String name) {
        return this.get(Condition.equal(RegionField.NAME, name));
    }
    /**
     * Starts building a fluent query.
     *
     * @return query builder
     */
    default FluidRegionRepository.QueryBuilder where() {
        return new FluidRegionRepository.QueryBuilder(this);
    }
    /**
     * Starts building a fluent query with initial conditions.
     *
     * @param conditions conditions to apply
     * @return query builder
     */
    default FluidRegionRepository.QueryBuilder where(Condition... conditions) {
        return new FluidRegionRepository.QueryBuilder(this).and(conditions);
    }
    /**
     * Returns regions that contain a location.
     *
     * @param point location to test
     * @return list of matching regions
     */
    default List<Region> getAt(Location point) {
        return this.get(Condition.at(point));
    }
    /**
     * Returns regions that contain a coordinate.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @param world world to test
     * @return list of matching regions
     */
    default List<Region> getAt(double x, double y, double z, World world) {
        return this.get(Condition.at(x, y, z, world));
    }
    /**
     * Returns regions that intersect an area.
     *
     * @param area area to test
     * @return list of matching regions
     */
    default List<Region> getIn(Area area) {
        return this.get(Condition.in(area));
    }
    /**
     * Returns regions that intersect a bounding box.
     *
     * @param x1 min X
     * @param y1 min Y
     * @param z1 min Z
     * @param x2 max X
     * @param y2 max Y
     * @param z2 max Z
     * @param world world to test
     * @return list of matching regions
     */
    default List<Region> getIn(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        return this.get(Condition.in(x1, y1, z1, x2, y2, z2, world));
    }
    /**
     * Returns regions in a world.
     *
     * @param world world to test
     * @return list of matching regions
     */
    default List<Region> getIn(World world) {
        return this.get(Condition.in(world));
    }
    /**
     * Returns regions intersecting a bounding box.
     *
     * @param boundingBox bounding box to test
     * @param world world to test
     * @return list of matching regions
     */
    default List<Region> getIn(BoundingBox boundingBox, World world) {
        return this.get(Condition.in(boundingBox, world));
    }
    /**
     * Returns regions intersecting another region.
     *
     * @param other region to test
     * @return list of matching regions
     */
    default List<Region> getIn(Region other) {
        return this.get(Condition.in(other));
    }
    /**
     * Returns regions intersecting a chunk coordinate.
     *
     * @param x chunk X
     * @param z chunk Z
     * @param world world to test
     * @return list of matching regions
     */
    default List<Region> inChunk(int x, int z, World world) {
        return this.get(Condition.inChunk(x, z, world));
    }
    /**
     * Returns regions intersecting a chunk.
     *
     * @param chunk chunk to test
     * @return list of matching regions
     */
    default List<Region> inChunk(Chunk chunk) {
        return this.get(Condition.inChunk(chunk));
    }

}
