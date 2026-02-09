package com.kntrel.mc.regionLib.test;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.test.mock.MockWorld;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import java.util.List;
import java.util.stream.Stream;

public final class Regions {

    private Regions() {}

    public static Region newRegion(RegionContext ctx, Hierarchy hierarchy, String name) {
        World w = ctx.getServer().getWorlds().getFirst();
        return new Region(ctx, new BoundingBox(0,0,0,1,1,1), w, name, hierarchy);
    }

    public static List<Region> newRegions(RegionContext ctx, Hierarchy hierarchy, String... names) {
        return Stream.of(names)
                .map(name -> newRegion(ctx, hierarchy, name))
                .toList();
    }

    public static List<Region> newRegions(int count, RegionContext ctx, Hierarchy hierarchy, String baseName) {
        return Stream.iterate(1, n -> n + 1)
                .limit(count)
                .map(n -> newRegion(ctx, hierarchy, baseName + n))
                .toList();
    }



}