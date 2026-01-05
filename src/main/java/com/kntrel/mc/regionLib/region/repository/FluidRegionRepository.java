package com.kntrel.mc.regionLib.region.repository;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public final class FluidRegionRepository {

    private FluidRegionRepository() {}


    public static class QueryBuilder implements Finisher {
        //FIELDS
        private final RegionRepository repo_;
        private Condition.Or root_;
        private List<Condition> current_;
        private RegionField<? extends Comparable<?>> orderBy_;
        private boolean ascending_;
        private boolean includeDestroyed_;


        //CONSTRUCTOR
        public QueryBuilder(RegionRepository repo) {
            this.repo_ = repo;
            this.current_ = new ArrayList<>();
            this.root_ = null;
            this.orderBy_ = null;
            this.ascending_ = true;
            this.includeDestroyed_ = false;
        }


        //FLUID MODIFIERS
        public QueryBuilder and(Condition... condition) {
            this.current_.addAll(Arrays.asList(condition));
            return this;
        }
        public QueryBuilder or() {
            Condition prev = Condition.and(this.current_);
            this.current_.clear();
            this.root_ = (this.root_ == null)
                    ? Condition.or(List.of(prev))
                    : this.root_.or(prev);
            return this;
        }


        //FLUID CONDITIONS
        public <T> QueryBuilder equal(RegionField<T> field, T value) {
            this.current_.add(Condition.equal(field, value));
            return this;
        }
        public QueryBuilder isTrue(RegionField<Boolean> field) {
            this.current_.add(Condition.isTrue(field));
            return this;
        }
        public QueryBuilder isFalse(RegionField<Boolean> field) {
            this.current_.add(Condition.isFalse(field));
            return this;
        }
        public <T> QueryBuilder notEqual(RegionField<T> field, T value) {
            this.current_.add(Condition.notEqual(field, value));
            return this;
        }
        public <T extends Comparable<T>> QueryBuilder greaterThan(RegionField<T> field, T value) {
            this.current_.add(Condition.greaterThan(field, value));
            return this;
        }
        public <T extends Comparable<T>> QueryBuilder lessThan(RegionField<T> field, T value) {
            this.current_.add(Condition.lessThan(field, value));
            return this;
        }
        public QueryBuilder contains(RegionField<String> field, String value) {
            this.current_.add(Condition.contains(field, value));
            return this;
        }
        public QueryBuilder hasMember(Player player) {
            this.current_.add(Condition.hasMember(player));
            return this;
        }
        public QueryBuilder hasMemberWithLevel(Player player, int level) {
            this.current_.add(Condition.hasMemberWithLevel(player, level));
            return this;
        }
        public QueryBuilder isAllowed(Player player, Ability ability) {
            this.current_.add(Condition.isAllowed(player, ability));
            return this;
        }
        public QueryBuilder hasRule(String ruleName) {
            this.current_.add(Condition.hasRule(ruleName));
            return this;
        }
        public <T> QueryBuilder ruleIs(Rule<T> rule, T value) {
            this.current_.add(Condition.ruleIs(rule, value));
            return this;
        }
        public QueryBuilder hasDataKey(String key) {
            this.current_.add(Condition.hasDataKey(key));
            return this;
        }
        public QueryBuilder dataValueIs(String key, String value) {
            this.current_.add(Condition.dataValueIs(key, value));
            return this;
        }
        public QueryBuilder at(Location point) {
            this.current_.add(Condition.at(point));
            return this;
        }
        public QueryBuilder at(double x, double y, double z, World world) {
            this.current_.add(Condition.at(x, y, z, world));
            return this;
        }
        public QueryBuilder in(Area area) {
            this.current_.add(Condition.in(area));
            return this;
        }
        public QueryBuilder in(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
            this.current_.add(Condition.in(x1, y1, z1, x2, y2, z2, world));
            return this;
        }
        public QueryBuilder in(World world) {
            this.current_.add(Condition.in(world));
            return this;
        }
        public QueryBuilder in(BoundingBox boundingBox, World world) {
            this.current_.add(Condition.in(boundingBox, world));
            return this;
        }
        public QueryBuilder in(Region other) {
            this.current_.add(Condition.in(other));
            return this;
        }
        public QueryBuilder inChunk(int x, int z, World world) {
            this.current_.add(Condition.inChunk(x, z, world));
            return this;
        }
        public QueryBuilder inChunk(Chunk chunk) {
            this.current_.add(Condition.inChunk(chunk));
            return this;
        }
        public QueryBuilder nameIs(String name) {
            return this.equal(RegionField.NAME, name);
        }
        public QueryBuilder nameContains(String name) {
            return this.contains(RegionField.NAME, name);
        }
        public QueryBuilder idIs(Long id) {
            return this.equal(RegionField.ID, id);
        }
        public QueryBuilder isEnabled() {
            return this.isTrue(RegionField.ENABLED);
        }
        public QueryBuilder isDestroyed(boolean b) {
            this.includeDestroyed_ = b;
            return this.equal(RegionField.DESTROYED, b);
        }
        public QueryBuilder includeDestroyed() {
            this.includeDestroyed_ = true;
            return this;
        }


        //ORDERING
        public <T extends Comparable<T>> Finisher orderBy(RegionField<T> field) {
            this.orderBy_ = field;
            this.ascending_ = true;
            return this;
        }
        public <T extends Comparable<T>> Finisher orderByDesc(RegionField<T> field) {
            this.orderBy_ = field;
            this.ascending_ = false;
            return this;
        }


        //FINISHERS
        public List<Region> get() {
            Query query = buildQuery(-1);
            return this.repo_.get(query);
        }

        public List<Region> getFirst(int count) {
            Query query = buildQuery(count);
            return this.repo_.get(query);
        }

        public Optional<Region> getFirst() {
            return this.getFirst(1).stream().findFirst();
        }


        //PRIVATE
        private Query buildQuery(int limit) {
            Condition finished = Condition.and(this.current_);
            Condition finalCondition = this.root_ == null
                    ? finished
                    : this.root_.or(finished);
            return new Query(finalCondition, this.orderBy_, this.ascending_, limit, this.includeDestroyed_);
        }
    }

    public interface Finisher {
        List<Region> get();
        List<Region> getFirst(int count);
        Optional<Region> getFirst();

    }
}
