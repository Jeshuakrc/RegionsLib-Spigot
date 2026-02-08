package com.kntrel.mc.regionLib.provided;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.rule.DeclareRule;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Iterator;

import static com.kntrel.mc.regionLib.region.rule.Rule.*;

public final class Rules {

    private Rules() {}

    @DeclareRule
    public static final Rule<Boolean> ANIMALS_TAKE_DAMAGE = on(EntityDamageEvent.class)
            .when(e -> e.getEntity() instanceof Animals)
            .at(e -> e.getEntity().getLocation())
            .ifFalse()
            .thenCancel()
            .done();

    @DeclareRule
    public static final Rule<Float> FIRE_SPREAD_RATE = of(ValueType.FLOAT).on(BlockSpreadEvent.class)
            .when(e -> e.getSource().getType().equals(Material.FIRE))
            .then((v, e) -> {
                if (v == 1.0) { return; }
                if (v == 0.0) { e.setCancelled(true); return; }
                if (Math.random() > v) { e.setCancelled(true); }
            })
            .done();

    @DeclareRule
    public static final Rule<Boolean> FIRE_GRIEF = on(BlockBurnEvent.class)
                .ifFalse()
                .thenCancel()
            .alsoOn(BlockSpreadEvent.class)                             // If fire spreads but doesn't burn, it grows a lot.
                .ifFalse()                                              // unless FIRE_SPREAD_RATE is explicit, this rule cuts spreads in half
                .iff((v, e, r) -> !r.hasRule("FIRE_SPREAD_RATE"))
                .then(e -> {
                    if (Math.random() < .5) { e.setCancelled(true); }
                })
            .done();

    @DeclareRule
    public static final Rule<Boolean> EXPLOSION_GRIEF = on(EntityExplodeEvent.class)
            .in(e -> areaOfBlocks(e.blockList(), e.getEntity().getWorld()))
            .ifFalse()
            .then((v, e, r) -> removeBlocksThatTouchRegion(e.blockList(), r))
        .alsoOn(BlockExplodeEvent.class)
            .in(e -> areaOfBlocks(e.blockList(), e.getBlock().getWorld()))
            .ifFalse()
            .then((v, e, r) -> removeBlocksThatTouchRegion(e.blockList(), r))
        .done();


    //helpers
    private static @Nullable Area areaOfBlocks(Collection<Block> blocks, World world) {
        if (blocks.isEmpty()) { return null; }

        double  minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY,
                maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (Block block : blocks) {
            Area a = Area.ofBlock(block);
            if (a.getMinX() < minX) { minX = a.getMinX(); }
            if (a.getMinY() < minY) { minY = a.getMinY(); }
            if (a.getMinZ() < minZ) { minZ = a.getMinZ(); }
            if (a.getMaxX() > maxX) { maxX = a.getMaxX(); }
            if (a.getMaxY() > maxY) { maxY = a.getMaxY(); }
            if (a.getMaxZ() > maxZ) { maxZ = a.getMaxZ(); }
        }

        return new Area(minX, minY, minZ, maxX, maxY, maxZ, world);
    }
    private static void removeBlocksThatTouchRegion(Collection<Block> blocks, Region region) {
        if (blocks.isEmpty()) { return; }

        Area regionArea = Area.ofRegion(region);
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            Area blockArea = Area.ofBlock(block);
            if (blockArea.overlaps(regionArea)) {
                it.remove();
            }
        }
    }
}
