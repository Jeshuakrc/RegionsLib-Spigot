package com.kntrel.mc.regionLib.util;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

public class Area extends BoundingBox {

    //ASSETS
    public enum Corner {
        UP_NORTH_EAST(1, 1, 0),
        UP_NORTH_WEST(0 , 1, 0),
        UP_SOUTH_EAST(1 , 1, 1),
        UP_SOUTH_WEST(0 , 1, 1),
        DOWN_NORTH_EAST(1 , 0, 0),
        DOWN_NORTH_WEST(0 , 0, 0),
        DOWN_SOUTH_EAST(1 , 0, 1),
        DOWN_SOUTH_WEST(0 , 0, 1);

        private final byte modX_, modY_, modZ_;

        Corner(int modX, int modY, int modZ) {
            this.modX_ = (byte) modX;
            this.modY_ = (byte) modY;
            this.modZ_ = (byte) modZ;
        }
        public byte getModX() { return this.modX_; }
        public byte getModY() { return this.modY_; }
        public byte getModZ() { return this.modZ_; }
    }


    //FIELDS
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
    public static Area ofRegion(Region region) {
        return new Area(region.getBoundingBox(), region.getWorld());
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
    public Location getCornerLocation(Area.Corner corner) {
        Location loc = new Location(this.getWorld(), this.getMinX(), this.getMinY(), this.getMinZ());
        loc.add(this.getWidthX()*corner.getModX(), this.getHeight()*corner.getModY(), this.getWidthZ()*corner.getModZ());
        return loc;
    }


    //UTILITIES
    public boolean touchesChunk(int x, int z, World world) {
        if (!this.getWorld().equals(world)) {
            return false;
        }
        double chunkMinX = x * 16.0;
        double chunkMinZ = z * 16.0;
        double chunkMaxX = chunkMinX + 16.0;
        double chunkMaxZ = chunkMinZ + 16.0;

        return     this.getMinX() < chunkMaxX
                && this.getMaxX() > chunkMinX
                && this.getMinZ() < chunkMaxZ
                && this.getMaxZ() > chunkMinZ;
    }
    public boolean touchesChunk(Chunk chunk) {
        return this.touchesChunk(chunk.getX(), chunk.getZ(), chunk.getWorld());
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
