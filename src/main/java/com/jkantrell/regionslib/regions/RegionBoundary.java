package com.jkantrell.regionslib.regions;

import com.jkantrell.regionslib.io.ConfigManager;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class RegionBoundary {

    //FIELDS
    private Region region_;
    private List<Location> boundaries_;
    private int borderResolution_ = ConfigManager.getTotemBorderResolution() + 1;

    //CONSTRUCTORS
    RegionBoundary(Region region){
        region_ = region;
        setBoundarieLocs();
    }

    //SETTERS
    private void setBoundarieLocs(){
        boundaries_ = new ArrayList<>();
        RegionBoundingBox box = this.getRegion().getBoundingBox();

        double  minx = box.getMinX(),
                maxx = box.getMaxX(),
                miny = box.getMinY(),
                maxy = box.getMaxY(),
                minz = box.getMinZ(),
                maxz = box.getMaxZ(),
                distx = this.getDistance_(box.getWidthX()),
                disty = this.getDistance_(box.getHeight()),
                distz = this.getDistance_(box.getWidthZ());

        for(double x = minx; x <= maxx; x+=distx){
            for (double y = miny; y <= maxy; y+=disty) {
                double j;
                if(x == minx || x == maxx || y == miny || y == maxy){
                    j=distz;
                } else {
                    j=box.getWidthZ();
                }
                for (double z = minz; z <= maxz; z += j) {
                    Location loc = new Location(this.getRegion().getWorld(), x, y, z);
                    boundaries_.add(loc);
                }
            }
        }
    }

    //GETTERS
    public Region getRegion() {
        return region_;
    }
    public List<Location> getFullBoundaries(){
        return boundaries_;
    }
    public void recalculate(){
        setBoundarieLocs();
    }

    //PRIVATE METHODS
    private double getDistance_(double distance){

        return (1 + (distance%1)) / this.borderResolution_;
    }
}


