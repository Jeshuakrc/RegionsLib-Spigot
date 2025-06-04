package com.jkantrell.regionslib.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

public class Area extends BoundingBox {

    private World world_;


    //CONSTRUCTORS
    public static Area ofBlock(Block block) {
        return new Area(block.getBoundingBox(), block.getWorld());
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
}
