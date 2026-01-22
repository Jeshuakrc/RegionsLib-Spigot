package com.kntrel.mc.regionLib.region.repository;

import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public sealed interface Condition extends Predicate<Region> {

    //CONSTANTS
    Absolute FALSE = new Absolute(false), TRUE = new Absolute(true);

    // FACTORY
    static Condition.Not not(Condition condition) { return new Condition.Not(condition); }
    static Condition.And AND(Condition... conditions) { return new Condition.And(conditions); }
    static Condition.And AND(List<Condition> conditions) { return new Condition.And(conditions); }
    static Condition.Or OR(Condition... conditions) { return new Condition.Or(conditions); }
    static Condition.Or OR(List<Condition> conditions) { return new Condition.Or(conditions); }
    static <T> Condition.Equal<T> equal(RegionField<T> field, T value) { return new Condition.Equal<>(field, value); }
    static Condition isTrue(RegionField<Boolean> field) { return new Condition.Equal<>(field, true); }
    static Condition isFalse(RegionField<Boolean> field) { return new Condition.Equal<>(field, false); }
    static <T> Condition notEqual(RegionField<T> field, T value) { return new Condition.Not(new Condition.Equal<>(field, value)); }
    static <T extends Comparable<T>> Condition.GreaterThan<T> greaterThan(RegionField<T> field, T value) { return new Condition.GreaterThan<>(field, value); }
    static <T extends Comparable<T>> Condition.LessThan<T> lessThan(RegionField<T> field, T value) { return new Condition.LessThan<>(field, value); }
    static <T extends Comparable<T>> Condition.GreaterThanEqual<T> greaterThanEqual(RegionField<T> field, T value) { return new Condition.GreaterThanEqual<>(field, value); }
    static <T extends Comparable<T>> Condition.LessThanEqual<T> lessThanEqual(RegionField<T> field, T value) { return new Condition.LessThanEqual<>(field, value); }
    static Condition.Contains contains(RegionField<String> field, String value) { return new Condition.Contains(field, value); }
    static Condition.HasMember hasMember(Player player) { return new Condition.HasMember(player); }
    static Condition.HasMemberWithLevel hasMemberWithLevel(Player player, int level) { return new Condition.HasMemberWithLevel(player, level); }
    static Condition.IsAllowed isAllowed(Player player, Ability ability) { return new Condition.IsAllowed(player, ability); }
    static Condition.HasRule hasRule(String ruleName) { return new Condition.HasRule(ruleName); }
    static <T> Condition.RuleIs<T> ruleIs(Rule<T> rule, T value) { return new Condition.RuleIs<>(rule, value); }
    static Condition.HasDataKey hasDataKey(String key) { return new Condition.HasDataKey(key); }
    static Condition.DataValueIs dataValueIs(String key, JsonElement value) { return new Condition.DataValueIs(key, value); }
    static Condition.At at(Location point) { return new Condition.At(point); }
    static Condition.In in(Area area) { return new Condition.In(area); }
    static Condition.InChunk inChunk(int x, int z, World world) { return new Condition.InChunk(x, z, world); }
    static <T extends Comparable<T>> Condition between(RegionField<T> field, T minValue, T maxValue) {
        return new Condition.And(
                new Condition.GreaterThanEqual<>(field, minValue),
                new Condition.LessThanEqual<>(field, maxValue)
        );
    }
    static Condition.At at(double x, double y, double z, World world) {
        return new Condition.At(new Location(world, x, y, z));
    }
    static Condition.In in(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        Area area = new Area(x1, y1, z1, x2, y2, z2, world);
        return new Condition.In(area);
    }
    static Condition.Equal<World> in(World world) {
        return new Condition.Equal<>(RegionField.WORLD, world);
    }
    static Condition.In in(BoundingBox boundingBox, World world) {
        return new Condition.In(new Area(boundingBox, world));
    }
    static Condition.In in(Region other) {
        return new Condition.In(Area.ofRegion(other));
    }
    static Condition.InChunk inChunk(Chunk chunk) {
        return new Condition.InChunk(chunk.getX(), chunk.getZ(), chunk.getWorld());
    }


    // CONTRACT
    default Condition and(Condition condition) {
        return new Condition.And(this, condition);
    }
    default Condition or(Condition condition) {
        return new Condition.Or(this, condition);
    }
    default Condition not() {
        return new Condition.Not(this);
    }


    // IMPLEMENTATIONS
    final class Absolute implements Condition {
        private final boolean value_;

        private Absolute(boolean value) { this.value_ = value; }
        public boolean getValue() { return this.value_; }
        @Override public boolean test(Region region) { return this.value_; }
    }

    sealed interface Composed extends Condition {
        List<Condition> conditions();
    }
    record And(List<Condition> conditions) implements Composed {
        public And(List<Condition> conditions) {
            this.conditions = List.copyOf(conditions);
        }
        public And(Condition... conditions) {
            this(List.of(conditions));
        }
        @Override public And and(Condition condition) {
            return new And(Stream.concat(this.conditions().stream(), Stream.of(condition)).toList());
        }
        @Override public boolean test(Region region) {
            for (Condition condition : this.conditions()) {
                if (!condition.test(region)) {
                    return false;
                }
            }
            return true;
        }
    }
    record Or(List<Condition> conditions) implements Composed {
        public Or(List<Condition> conditions) {
            this.conditions = List.copyOf(conditions);
        }
        public Or(Condition... conditions) {
            this(List.of(conditions));
        }
        @Override public Or or(Condition condition) {
            return new Or(Stream.concat(this.conditions().stream(), Stream.of(condition)).toList());
        }
        @Override public boolean test(Region region) {
            for (Condition condition : this.conditions()) {
                if (condition.test(region)) {
                    return true;
                }
            }
            return false;
        }
    }
    record Not(Condition condition) implements Condition {
        @Override public Condition not() {
            return this.condition();
        }
        @Override public boolean test(Region region) {
            return !this.condition().test(region);
        }
    }

    sealed interface Comparative<T> extends Condition {
        RegionField<T> field();
        T value();
    }
    record Equal<T>(RegionField<T> field, T value) implements Comparative<T> {
        @Override public boolean test(Region region) {
            T val = region.getField(this.field());
            if (val == null && this.value() == null) { return true; }
            if (val == null || this.value() == null) { return false; }
            return val.equals(this.value());
        }
    }
    record GreaterThan<T extends Comparable<T>>(RegionField<T> field, T value) implements Comparative<T> {
        @Override public boolean test(Region region) {
            T val = region.getField(this.field());
            if (val == null || this.value() == null) { return false; }
            return val.compareTo(this.value()) > 0;
        }
    }
    record LessThan<T extends Comparable<T>>(RegionField<T> field, T value) implements Comparative<T> {
        @Override public boolean test(Region region) {
            T val = region.getField(this.field());
            if (val == null || this.value() == null) { return false; }
            return val.compareTo(this.value()) < 0;
        }
    }
    record GreaterThanEqual<T extends Comparable<T>>(RegionField<T> field, T value) implements Comparative<T> {
        @Override public boolean test(Region region) {
            T val = region.getField(this.field());
            if (val == null || this.value() == null) { return false; }
            return val.compareTo(this.value()) >+ 0;
        }
    }
    record LessThanEqual<T extends Comparable<T>>(RegionField<T> field, T value) implements Comparative<T> {
        @Override public boolean test(Region region) {
            T val = region.getField(this.field());
            if (val == null || this.value() == null) { return false; }
            return val.compareTo(this.value()) <= 0;
        }
    }
    record Contains(RegionField<String> field, String value) implements Comparative<String> {
        @Override public boolean test(Region region) {
            String val = region.getField(this.field());
            if (val == null || this.value() == null) { return false; }
            return val.contains(this.value());
        }
    }

    sealed interface Relational extends Condition {}
    record HasMember(Player player) implements Relational {
        @Override public boolean test(Region region) {
            return region.isMember(this.player());
        }
    }
    record HasMemberWithLevel(Player player, int level) implements Relational {
        @Override public boolean test(Region region) {
            return region.getPermissions(this.player()).stream()
                    .anyMatch(p -> p.getGroup().getLevel() >= this.level());
        }
    }
    record IsAllowed(Player player, Ability ability) implements Relational {
        @Override public boolean test(Region region) {
            return region.checkAbility(this.player(), this.ability());
        }
    }
    record HasRule(String ruleName) implements Relational {
        @Override public boolean test(Region region) {
            return region.hasRule(this.ruleName());
        }
    }
    record RuleIs<T>(Rule<T> rule, T value) implements Relational {
        @Override public boolean test(Region region) {
            T val = region.getRuleValue(this.rule()).orElse(null);
            if (val == null && this.value() == null) { return true; }
            if (val == null || this.value() == null) { return false; }
            return val.equals(this.value());
        }
    }
    record HasDataKey(String key) implements Relational {
        @Override public boolean test(Region region) {
            return region.getDataContainer().has(this.key());
        }
    }
    record DataValueIs(String key, JsonElement value) implements Relational {
        @Override public boolean test(Region region) {
            RegionData data = region.getDataContainer().get(this.key());
            JsonElement val = (data == null) ? null : data.getValue();
            if (val == null && this.value() == null) { return true; }
            if (val == null || this.value() == null) { return false; }
            return val.equals(this.value());
        }
    }

    sealed interface Positional extends Condition {}
    record At(Location point) implements Positional {
        @Override public boolean test(Region region) {
            return region.contains(this.point());
        }
    }
    record In(Area area) implements Positional {
        @Override public boolean test(Region region) {
            return region.contains(this.area());
        }
    }
    record InChunk(int x, int z, World world) implements Positional {
        @Override public boolean test(Region region) {
            if (!this.world().getUID().equals(region.getWorld().getUID())) { return false; }
            return region.touchesChunk(this.x(), this.z());
        }
    }
}
