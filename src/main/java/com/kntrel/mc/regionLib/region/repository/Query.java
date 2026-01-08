package com.kntrel.mc.regionLib.region.repository;

import com.kntrel.mc.regionLib.region.RegionField;
import java.util.Optional;

public class Query {

    //FACTORY
    public static FluidRegionRepository.QueryBuilder builder(RegionRepository repo) {
        return new FluidRegionRepository.QueryBuilder(repo);
    }


    //ASSETS
    public record Ordering(RegionField<? extends Comparable<?>> field, boolean ascending) {};


    //FIELDS
    private final Condition condition_;
    private final RegionField<? extends Comparable<?>> orderBy_;
    private final boolean ascending_;
    private final int limit_;
    private final boolean includeDestroyed_;


    //CONSTRUCTORS
    public Query(Condition condition, RegionField<? extends Comparable<?>> orderBy, boolean ascending, int limit, boolean includeDestroyed) {
        this.condition_ = condition;
        this.orderBy_ = orderBy;
        this.ascending_ = ascending;
        this.limit_ = limit;
        this.includeDestroyed_ = includeDestroyed;
    }
    public Query(Condition condition, RegionField<? extends Comparable<?>> orderBy, boolean ascending, boolean includeDestroyed) {
        this(condition, orderBy, ascending, -1, includeDestroyed);

    }
    public Query(Condition condition, RegionField<? extends Comparable<?>> orderBy, boolean includeDestroyed) {
        this(condition, orderBy, true, -1, includeDestroyed);
    }
    public Query(Condition condition, int limit, boolean includeDestroyed) {
        this(condition, null, true, limit, includeDestroyed);
    }
    public Query(Condition condition, boolean includeDestroyed) {
        this(condition, null, true, -1, includeDestroyed);
    }


    //GETTERS
    public Condition getCondition() {
        return this.condition_;
    }
    public Optional<Ordering> getOrdering() {
        if (this.orderBy_ == null) { return Optional.empty(); }
        return Optional.of(new Ordering(this.orderBy_, this.ascending_));
    }
    public int getLimit() {
        return this.limit_;
    }
    public boolean includesDestroyed() {
        return this.includeDestroyed_;
    }
}
