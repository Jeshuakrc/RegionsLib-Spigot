package com.kntrel.mc.regionLib.test.util;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockPlugin;
import org.bukkit.plugin.Plugin;
import java.util.List;

public class TestRegionContext extends RegionContext {

    //CONSTANTS
    private static final String NAMESPACE = "test";


    public TestRegionContext(Plugin plugin, RegionContextConfig config, RegionRepository regionRepo, HierarchyRepository hierarchyRepo) {
        super(NAMESPACE, config, plugin, ctx -> regionRepo, ctx -> hierarchyRepo);
    }

    public TestRegionContext(RegionContextConfig config) {
        this(new MockPlugin(), config, new MemoryRegionRepository(), new MockHierarchyRepository(List.of()));
    }

    public TestRegionContext() {
        this(RegionContextConfig.defaultConfig());
    }


    @Override
    public RegionReadRepository getHotRegionRepository() {
        return this.getRegionRepository();
    }
}
