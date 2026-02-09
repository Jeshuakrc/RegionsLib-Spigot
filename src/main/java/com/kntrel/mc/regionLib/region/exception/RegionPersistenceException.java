package com.kntrel.mc.regionLib.region.exception;

import com.kntrel.mc.regionLib.region.Region;

/**
 * Base exception for persistence-related region operations.
 */
public abstract class RegionPersistenceException extends RegionException {
    public RegionPersistenceException(Region region) {
        super(region);
    }
    public RegionPersistenceException(Region region, String message) {
        super(region, message);
    }
    public RegionPersistenceException(Region region, Throwable cause) {
        super(region, cause);
    }
    public RegionPersistenceException(Region region, String message, Throwable cause) {
        super(region, message, cause);
    }
}
