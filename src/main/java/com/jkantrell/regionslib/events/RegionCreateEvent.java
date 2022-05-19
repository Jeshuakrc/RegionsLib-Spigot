package com.jkantrell.regionslib.events;

import com.jkantrell.regionslib.regions.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import javax.annotation.Nonnull;

public class RegionCreateEvent extends Event implements Cancellable {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    @Nonnull
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    //===============================================================

    //FIELDS
    private boolean canceled_ = false;
    private final Region region_;
    private final Entity creator_;

    //CONSTRUCTOR
    public RegionCreateEvent(Region region, Entity creator) {
        this.region_ = region;
        this.creator_ = creator;
    }

    //GETTERS
    @Override
    public boolean isCancelled() {
        return this.canceled_;
    }
    public Entity getCreator() {
        return this.creator_;
    }
    public Region getRegion() {
        return this.region_;
    }

    //SETTERS
    @Override
    public void setCancelled(boolean b) {
        this.canceled_ = b;
    }
}
