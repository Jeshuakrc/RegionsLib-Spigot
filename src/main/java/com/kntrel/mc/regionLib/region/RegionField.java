package com.kntrel.mc.regionLib.region;

import org.bukkit.World;
import java.util.function.Function;

public class RegionField<T> {

    //CONSTANTS
    public static final RegionField<Long> ID = new RegionField<>("id", Region::getId);
    public static final RegionField<World> WORLD = new RegionField<>("world", Region::getWorld);
    public static final RegionField<String> NAME = new RegionField<>("name", Region::getName);
    public static final RegionField<Boolean> ENABLED = new RegionField<>("enabled", Region::isEnabled);
    public static final RegionField<Boolean> DESTROYED = new RegionField<>("destroyed", Region::isDestroyed);
    public static final RegionField<Double> MIN_X = new RegionField<>("min_x", r -> r.getBoundingBox().getMinX());
    public static final RegionField<Double> MIN_Y = new RegionField<>("min_y", r -> r.getBoundingBox().getMinY());
    public static final RegionField<Double> MIN_Z = new RegionField<>("min_z", r -> r.getBoundingBox().getMinZ());
    public static final RegionField<Double> MAX_X = new RegionField<>("max_x", r -> r.getBoundingBox().getMaxX());
    public static final RegionField<Double> MAX_Y = new RegionField<>("max_y", r -> r.getBoundingBox().getMaxY());
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
    public String getName() {
        return this.name_;
    }

    //USAGE
    public T extract(Region region) {
        return this.getter_.apply(region);
    }
}
