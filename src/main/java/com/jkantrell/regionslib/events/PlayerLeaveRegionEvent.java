package com.jkantrell.regionslib.events;

import com.jkantrell.regionslib.regions.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

public class PlayerLeaveRegionEvent extends PlayerEnterRegionEvent {
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

    public PlayerLeaveRegionEvent(Player who, Region region) {
        super(who, region);
    }
}
