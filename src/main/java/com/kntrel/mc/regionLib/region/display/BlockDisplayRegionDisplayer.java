package com.kntrel.mc.regionLib.region.display;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;

/**
 * Displays region boundaries using {@link org.bukkit.entity.BlockDisplay} entities.
 */
public class BlockDisplayRegionDisplayer implements RegionDisplayer {

    private final RegionContext ctx_;
    private final Map<Long, BlockDisplay[]> displayMap_;
    private long nextId_;


    public BlockDisplayRegionDisplayer(RegionContext regionContext) {
        this.ctx_ = regionContext;
        this.displayMap_ = new HashMap<>();
        this.nextId_ = Long.MIN_VALUE;
    }


    @Override public DisplayToken display(Region region, @Nullable Player player) {
        Area area = Area.ofRegion(region);

        Location origin = (player != null)
                ? player.getLocation()
                : new Location(area.getWorld(), area.getCenterX(), area.getCenterY(), area.getCenterZ());

        Vector2f xSize = new Vector2f((float) area.getWidthZ(), (float) area.getHeight()),
                 ySize = new Vector2f((float) area.getWidthX(), (float) area.getWidthZ()),
                 zSize = new Vector2f((float) area.getWidthX(), (float) area.getHeight());

        boolean visible = player == null;

        BlockDisplay[] entities = new BlockDisplay[] {
            this.spawnFace(region, area.getCornerLocation(Area.Corner.UP_NORTH_WEST), origin, BlockFace.UP, ySize, visible),
            this.spawnFace(region, area.getCornerLocation(Area.Corner.DOWN_NORTH_WEST), origin, BlockFace.DOWN, ySize, visible),
            this.spawnFace(region, area.getCornerLocation(Area.Corner.DOWN_NORTH_WEST), origin, BlockFace.WEST, xSize, visible),
            this.spawnFace(region, area.getCornerLocation(Area.Corner.DOWN_NORTH_EAST), origin, BlockFace.EAST, xSize, visible),
            this.spawnFace(region, area.getCornerLocation(Area.Corner.DOWN_NORTH_WEST), origin, BlockFace.NORTH, zSize, visible),
            this.spawnFace(region, area.getCornerLocation(Area.Corner.DOWN_SOUTH_WEST), origin, BlockFace.SOUTH, zSize, visible),
        };

        if (!visible) {
            for (Entity e : entities) { player.showEntity(this.ctx_.getPlugin(), e); }
        }

        long id;
        synchronized (this.displayMap_) {
            id = this.nextId_++;
            this.displayMap_.put(id, entities);
        }

        return new DisplayToken(this, id);
    }

    @Override public DisplayToken display(Region region) {
        return this.display(region, null);
    }

    @Override public void stop(DisplayToken token) {
        if (!token.displayer().equals(this)) { return; }

        Entity[] entities = this.displayMap_.get(token.id());
        if (entities == null) { return; }

        for (Entity e : entities) { e.remove(); }
    }


    //PRIVATE
    protected BlockData getFaceBlockData(Region region, BlockFace facing) {
        return Material.CYAN_STAINED_GLASS.createBlockData();
    }
    private BlockDisplay spawnFace(Region region, Location corner, Location origin, BlockFace facing, Vector2f size, boolean visible) {
        assert corner.getWorld() != null;

        Vector3f tr = new Vector3f(
                (float)(corner.getX() - origin.getX()),
                (float)(corner.getY() - origin.getY()),
                (float)(corner.getZ() - origin.getZ())
        );

        Vector3f scale = new Vector3f(size.x(), size.y(), .1f);
        Quaternionf rot = new Quaternionf();
        switch (facing) {
            case UP, DOWN -> rot.rotateX((float) Math.PI / 2);
            case EAST, WEST -> rot.rotateY(-(float) Math.PI / 2);
        };

        // small extra offset so DOWN, WEST, SOUTH faces sit on the outside
        switch (facing) {
            case DOWN -> tr.add(0,  .1f, 0);
            case WEST -> tr.add(.1f,0, 0);
            case SOUTH -> tr.add(0, 0, -.1f);
        }

        return corner.getWorld().spawn(origin, BlockDisplay.class, d -> {
            d.setBlock(this.getFaceBlockData(region, facing));
            d.setPersistent(false);
            d.setViewRange(8f);
            d.setTransformation(new Transformation(tr, rot, scale, new Quaternionf()));
            d.setVisibleByDefault(visible);
        });
    }
}
