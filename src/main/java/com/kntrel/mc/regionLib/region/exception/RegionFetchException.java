package com.kntrel.mc.regionLib.region.exception;



public abstract class RegionFetchException extends RuntimeException {

    //CONSTRUCTORS
    public RegionFetchException() {
        super();
    }
    public RegionFetchException(String message) {
        super(message);
    }
    public RegionFetchException(Throwable cause) {
        super(cause);
    }
    public RegionFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
