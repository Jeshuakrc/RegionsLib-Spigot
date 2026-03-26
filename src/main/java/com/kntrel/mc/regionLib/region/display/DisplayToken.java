package com.kntrel.mc.regionLib.region.display;

/**
 * Identifies an active display instance.
 *
 * @param displayer displayer used
 * @param id display id
 */
public record DisplayToken(RegionDisplayer displayer, long id) {}
