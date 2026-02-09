package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base Bukkit event for actions scoped to a {@link com.kntrel.mc.regionLib.region.Region}.
 */
public abstract class RegionEvent extends Event {

    //FIELDS
    private final Region region_;


    //CONSTRUCTORS
    /**
     * Creates a region event for the supplied region.
     *
     * @param region region the event targets
     */
    public RegionEvent(@NotNull Region region) {
        this.region_ = region;
    }


    //GETTERS
    /**
     * Returns the region associated with the event.
     *
     * @return the region instance
     */
    @NotNull public Region getRegion() {
        return this.region_;
    }

}
