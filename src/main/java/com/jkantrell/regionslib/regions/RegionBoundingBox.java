package com.jkantrell.regionslib.regions;

import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class RegionBoundingBox extends BoundingBox {

    //FIELDS
    private final Region region_;

    //CONSTRUCTOR
    RegionBoundingBox(double x1, double y1, double z1, double x2, double y2, double z2, Region region) {
        super(x1, y1, z1, x2, y2, z2);
        this.region_=region;
    }
    RegionBoundingBox(Region region){
        region_ = region;
    }

    //PUBLIC METHODS
    public Region getRegion(){
        return region_;
    }
    public double[] getCorners() {
        return new double[] {
                this.getMinX(), this.getMinY(), this.getMinZ(),
                this.getMaxX(), this.getMaxY(), this.getMaxZ()
        };
    }

    //OVERWRITEN METHODS
    @Override public RegionBoundingBox expand(double negativeX, double negativeY, double negativeZ, double positiveX, double positiveY, double positiveZ) {
        super.expand(negativeX, negativeY, negativeZ, positiveX, positiveY, positiveZ);
        setRegionVertex_();
        return this;
    }
    @Override public RegionBoundingBox expand(double x, double y, double z) {
        super.expand(x, y, z);
        setRegionVertex_();
        return this;
    }
    @Override public RegionBoundingBox expand(Vector expansion) {
        super.expand(expansion);
        setRegionVertex_();
        return this;
    }
    @Override public RegionBoundingBox expand(double expansion) {
        super.expand(expansion);
        setRegionVertex_();
        return this;
    }
    @Override public RegionBoundingBox expand(double dirX, double dirY, double dirZ, double expansion) {
        super.expand(dirX, dirY, dirZ, expansion);
        setRegionVertex_();
        return this;
    }
    @Override public RegionBoundingBox expand(Vector direction, double expansion) {
        super.expand(direction, expansion);
        setRegionVertex_();
        return this;
    }
    @Override public RegionBoundingBox expand(BlockFace blockFace, double expansion) {
        super.expand(blockFace, expansion);
        setRegionVertex_();
        return this;
    }
    @Override public RegionBoundingBox expandDirectional(double dirX, double dirY, double dirZ) {
        super.expandDirectional(dirX, dirY, dirZ);
        setRegionVertex_();
        return this;
    }
    @Override public RegionBoundingBox expandDirectional(Vector direction) {
        super.expandDirectional(direction);
        setRegionVertex_();
        return this;
    }

    //PRIVATE METHODS
    private void setRegionVertex_(){
        this.getRegion().setVertex(this.getCorners());
    }
}
