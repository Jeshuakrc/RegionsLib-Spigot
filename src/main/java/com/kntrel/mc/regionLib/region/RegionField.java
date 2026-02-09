package com.kntrel.mc.regionLib.region;

import org.bukkit.World;
import java.util.function.Function;

/**
 * Describes a named, extractable field from a {@link Region}.
 *
 * @param <T> field value type
 */
public class RegionField<T> {

    //CONSTANTS
    /** Field for region ids. */
    public static final RegionField<Long> ID = new RegionField<>("id", Region::getId);
    /** Field for region worlds. */
    public static final RegionField<World> WORLD = new RegionField<>("world", Region::getWorld);
    /** Field for region names. */
    public static final RegionField<String> NAME = new RegionField<>("name", Region::getName);
    /** Field for region enabled state. */
    public static final RegionField<Boolean> ENABLED = new RegionField<>("enabled", Region::isEnabled);
    /** Field for region destroyed state. */
    public static final RegionField<Boolean> DESTROYED = new RegionField<>("destroyed", Region::isDestroyed);
    /** Field for minimum X bound. */
    public static final RegionField<Double> MIN_X = new RegionField<>("min_x", r -> r.getBoundingBox().getMinX());
    /** Field for minimum Y bound. */
    public static final RegionField<Double> MIN_Y = new RegionField<>("min_y", r -> r.getBoundingBox().getMinY());
    /** Field for minimum Z bound. */
    public static final RegionField<Double> MIN_Z = new RegionField<>("min_z", r -> r.getBoundingBox().getMinZ());
    /** Field for maximum X bound. */
    public static final RegionField<Double> MAX_X = new RegionField<>("max_x", r -> r.getBoundingBox().getMaxX());
    /** Field for maximum Y bound. */
    public static final RegionField<Double> MAX_Y = new RegionField<>("max_y", r -> r.getBoundingBox().getMaxY());
    /** Field for maximum Z bound. */
    public static final RegionField<Double> MAX_Z = new RegionField<>("max_z", r -> r.getBoundingBox().getMaxZ());

    //FIELDS
    private final String name_;
    private final Function<Region, T> getter_;

    //CONSTRUCTOR
    private RegionField(String name, Function<Region, T> getter) {
        this.name_ = name;
        this.getter_ = getter;
    }

    //GETTERS
    /**
     * Returns the field name.
     *
     * @return field name
     */
    public String getName() {
        return this.name_;
    }

    //USAGE
    /**
     * Extracts the field value from a region.
     *
     * @param region region to read from
     * @return field value
     */
    public T extract(Region region) {
        return this.getter_.apply(region);
    }
}
