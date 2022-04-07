package com.jkantrell.regionslib.events;

import com.jkantrell.regionslib.regions.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import javax.annotation.Nonnull;

public class PlayerEnterRegionEvent extends PlayerEvent {
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
    private final Region region_;
    private final Location location_;

    //CONSTRUCTORS
    public PlayerEnterRegionEvent(Player who, Region region) {
        super(who);
        this.location_ = who.getLocation();
        this.region_ = region;

    }

    //GETTERS
    public Region getRegion() {
        return this.region_;
    }
    public Location getLocation() {
        return this.location_;
    }

}
