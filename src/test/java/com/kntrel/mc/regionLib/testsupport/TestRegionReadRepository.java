package com.kntrel.mc.regionLib.testsupport;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;

import java.util.ArrayList;
import java.util.List;

public class TestRegionReadRepository implements RegionReadRepository {

    private final List<Region> regions = new ArrayList<>();

    public void setRegions(List<Region> regions) {
        this.regions.clear();
        this.regions.addAll(regions);
    }

    @Override
    public List<Region> get(Query query) {
        return List.copyOf(this.regions);
    }
}
