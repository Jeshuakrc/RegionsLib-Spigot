package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import org.jetbrains.annotations.NotNull;

public class PlayerLeaveRegionEvent extends PlayerEnterRegionEvent {
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

    public PlayerLeaveRegionEvent(Player who, Region region) {
        super(who, region);
    }
}
