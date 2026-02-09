package com.kntrel.mc.regionLib.region.listen;

import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Represents either a point or area location used by triggers.
 */
public class Place {

    //FACTORY
    public static Place ofArea(Area area) {
        return new Place(area, null);
    }
    public static Place ofPoint(Location point) {
        if (point.getWorld() == null) {
            throw new IllegalArgumentException("Point location must have a world.");
        }
        return new Place(null, point);
    }


    //FIELDS
    private final @Nullable Area area_;
    private final @Nullable Location point_;


    //CONSTRUCTORS
    private Place(@Nullable Area area, @Nullable Location point) {
        this.area_ = area;
        this.point_ = point;
    }


    //GETTERS
    public boolean isArea() {
        return this.area_ != null;
    }
    public boolean isPoint() {
        return this.point_ != null;
    }
    public @Nullable Area getArea() {
        return this.area_;
    }
    public @Nullable Location getPoint() {
        return this.point_;
    }
}
