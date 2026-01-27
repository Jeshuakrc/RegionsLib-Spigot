package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class RegionEvent extends Event {

    //FIELDS
    private final Region region_;


    //CONSTRUCTORS
    public RegionEvent(@NotNull Region region) {
        this.region_ = region;
    }


    //GETTERS
    @NotNull public Region getRegion() {
        return this.region_;
    }

}
