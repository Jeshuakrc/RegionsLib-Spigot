package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.event.Event;

public abstract class RegionEvent extends Event {

    //FIELDS
    private final Region region_;


    //CONSTRUCTORS
    public RegionEvent(Region region) {
        this.region_ = region;
    }


    //GETTERS
    public Region getRegion() {
        return this.region_;
    }

}
