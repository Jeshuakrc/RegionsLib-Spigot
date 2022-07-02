package com.jkantrell.regionslib.regions;

import com.jkantrell.regionslib.io.Serializer;
import com.jkantrell.regionslib.regions.rules.RuleDataType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class Regions {
    //STATIC FIELDS
    static List<Region> regions_ = new ArrayList<>();

    //STATIC METHODS
    public static Optional<Region> get(int id) {
        return regions_.stream().filter(r -> r.getId() == id).findFirst();
    }
    public static Region[] get(String name) {
        return getAll(r -> r.getName().equals(name));
    }
    public static Region[] loadAll() {
        regions_ = Serializer.deserializeFileList(Serializer.FILES.REGIONS, Region.class);
        return regions_.toArray(new Region[0]);
    }
    public static Region[] getAll() {
        return getAll(r -> true);
    }
    public static Region[] getAll(Predicate<Region> condition) {
        return regions_.stream().filter(condition).toArray(Region[]::new);
    }
    public static Region[] getAt(double x, double y, double z, World world, Predicate<Region> condition){
        return getAllAt(x,y,z,world, r -> r.isEnabled() && condition.test(r));
    }
    public static Region[] getAt(Location location, Predicate<Region> condition){
        return getAt(location.getX(),location.getY(),location.getZ(), Objects.requireNonNull(location.getWorld()),condition);
    }
    public static Region[] getAt(double x, double y, double z, World world) {
        return getAt(x, y, z, world, r -> true);
    }
    public static Region[] getAt(Location location) {
        return getAt(location, r -> true);
    }
    public static Region[] getAllAt(double x, double y, double z, World world) {
        return getAllAt(x,y,z,world, r -> true);
    }
    public static Region[] getAllAt(Location location) {
        return getAllAt(location, r -> true);
    }
    public static Region[] getAllAt(double x, double y, double z, World world, Predicate<Region> condition) {
        return getAll(r -> r.contains(x,y,z,world) && condition.test(r));
    }
    public static Region[] getAllAt(Location location, Predicate<Region> condition) {
        return getAllAt(location.getX(),location.getY(),location.getZ(), Objects.requireNonNull(location.getWorld()),condition);
    }
    public static Region[] getIn(BoundingBox boundingBox, World world, Predicate<Region> condition) {
        return getAllIn(boundingBox, world, r -> r.isEnabled() && condition.test(r));
    }
    public static Region[] getIn(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        return getIn(new BoundingBox(x1, y1, z1, x2, y2, z2), world);
    }
    public static Region[] getIn(Region region) {
        return getIn(region.getBoundingBox(),region.getWorld(), r -> !r.equals(region));
    }
    public static Region[] getIn(BoundingBox boundingBox, World world) {
        return getIn(boundingBox, world, r -> true);
    }
    public static Region[] getAllIn(BoundingBox boundingBox, World world, Predicate<Region> condition) {
        return getAll(r -> r.getBoundingBox().overlaps(boundingBox) && r.getWorld().equals(world) && condition.test(r));
    }
    public static Region[] getAllIn(BoundingBox boundingBox, World world) {
        return getAllIn(boundingBox, world, r -> true);
    }
    public static Region[] getRuleContainersAt(String ruleName, RuleDataType dataType, Location location) {
        return getAt(location, region -> region.hasRule(ruleName, dataType));
    }
    public static int getHighestId() {
        return regions_.stream().map(Region::getId).max(Integer::compare).orElse(0);
    }
}