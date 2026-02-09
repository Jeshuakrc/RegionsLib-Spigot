package com.kntrel.mc.regionLib.test.util;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import java.util.*;
import java.util.stream.Stream;

public class MemoryRegionRepository implements RegionRepository {

    //FIELDS
    private final Set<Region> store_;
    private long nextId_ = 1L;


    //CONSTRUCTORS
    public MemoryRegionRepository() {
        this.store_ = new HashSet<>();
    }


    @Override
    public List<Region> get(Query query) {
        Stream<Region> stream = this.store_.stream();

        Condition cond = query.getCondition();
        if (cond != null) {
            stream = stream.filter(cond);
        }

        Query.Ordering ordering = query.getOrdering().orElse(null);
        if (ordering != null) {
            final RegionField<? extends Comparable<?>> field = ordering.field();
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Comparator<Region> cmp = Comparator.comparing(r -> (Comparable) field.extract(r));
            if (!ordering.ascending()) {
                cmp = cmp.reversed();
            }
            stream = stream.sorted(cmp);
        }

        int limit = query.getLimit();
        if (limit > 0) {
            stream = stream.limit(limit);
        }

        if (!query.includesDestroyed()) {
            stream = stream.filter(r -> !r.isDestroyed());
        }

        return stream.toList();
    }

    @Override
    public void save(Region... region) {
        for (Region r : region) {
            if (r.getId() == null) {
                r.setId(nextId_++);
            }
            this.store_.remove(r);
            this.store_.add(r);
        }
    }
}
