package com.kntrel.mc.regionLib.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

public class Area extends BoundingBox {

    private World world_;


    //CONSTRUCTORS
    public static Area ofBlock(Block block) {
        BoundingBox bb = block.getBoundingBox();
        if (nullBoundingBox(bb)) {
            int x = block.getX(), y = block.getY(), z = block.getZ();
            bb = new BoundingBox(x, y, z, x + 1, y + 1, z + 1);
        }
        return new Area(bb, block.getWorld());
    }
    public Area(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        super(x1, y1, z1, x2, y2, z2);
        this.world_ = world;
    }
    public Area(BoundingBox base, World world) {
        this(base.getMinX(), base.getMinY(), base.getMinZ(), base.getMaxX(), base.getMaxY(), base.getMaxZ(), world);
    }


    //GETTERS
    public World getWorld() {
        return this.world_;
    }

    public Location middle() {
        return new Location(this.getWorld(), this.getCenterX(), this.getCenterY(), this.getCenterZ());
    }

    //SETTERS
    public void setWorld(World world) {
        this.world_ = world;
    }


    //PRIVATE
    private static boolean nullBoundingBox(BoundingBox bb) {
        return     bb.getMinX() == bb.getMaxX()
                && bb.getMaxX() == bb.getMinY()
                && bb.getMinY() == bb.getMaxY()
                && bb.getMaxY() == bb.getMinZ()
                && bb.getMinZ() == bb.getMaxZ();
    }
}
