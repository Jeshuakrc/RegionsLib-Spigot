package com.kntrel.mc.regionLib.region.exception;

import com.kntrel.mc.regionLib.region.Region;

/**
 * Base runtime exception for region-related operations.
 */
public abstract class RegionException extends RuntimeException {

    //FIELDS
    private final Region region_;


    //CONSTRUCTORS
    public RegionException(Region region) {
        super();
        this.region_ = region;
    }
    public RegionException(Region region, String message) {
        super(message);
        this.region_ = region;
    }
    public RegionException(Region region, Throwable cause) {
        super(cause);
        this.region_ = region;
    }
    public RegionException(Region region, String message, Throwable cause) {
        super(message, cause);
        this.region_ = region;
    }


    //GETTERS
    public Region getRegion() {
        return this.region_;
    }
}
