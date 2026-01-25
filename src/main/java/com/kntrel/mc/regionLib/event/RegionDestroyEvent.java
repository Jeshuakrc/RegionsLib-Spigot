package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.jetbrains.annotations.NotNull;

public class RegionDestroyEvent extends RegionEvent implements Cancellable {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    //===============================================================

    //FIELDS
    private boolean canceled_ = false;
    private final Entity destructor_;

    //CONSTRUCTOR
    public RegionDestroyEvent(Region region, Entity destructor) {
        super(region);
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

    //SETTERS
    @Override
    public void setCancelled(boolean b) {
        this.canceled_ = b;
    }
}
