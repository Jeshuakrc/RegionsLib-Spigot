package com.jkantrell.regionslib.events;

import com.jkantrell.regionslib.regions.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

public class RegionDestroyEvent extends Event implements Cancellable {
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
    private final Entity destructor_;

    //CONSTRUCTOR
    public RegionDestroyEvent(Region region, Entity destructor) {
        this.region_ = region;
        this.destructor_ = destructor;
    }

    //GETTERS
    @Override
    public boolean isCancelled() {
        return this.canceled_;
    }
    public Entity getDestructor() {
        return this.destructor_;
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
