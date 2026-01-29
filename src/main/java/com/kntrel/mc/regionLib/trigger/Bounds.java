package com.kntrel.mc.regionLib.trigger;

import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class Bounds {

    //FACTORY
    public static Bounds ofArea(Area area) {
        return new Bounds(area, null);
    }
    public static Bounds ofPoint(Location point) {
        if (point.getWorld() == null) {
            throw new IllegalArgumentException("Point location must have a world.");
        }
        return new Bounds(null, point);
    }


    //FIELDS
    private final @Nullable Area area_;
    private final @Nullable Location point_;


    //CONSTRUCTORS
    private Bounds(@Nullable Area area, @Nullable Location point) {
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
