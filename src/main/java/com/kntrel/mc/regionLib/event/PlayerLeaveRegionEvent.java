package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player leaves a region.
 */
public class PlayerLeaveRegionEvent extends PlayerEnterRegionEvent {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * Returns the static handler list for this event.
     *
     * @return handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    @NotNull
    /**
     * Returns the handler list for this event instance.
     *
     * @return handler list
     */
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    //===============================================================

    /**
     * Creates an event when a player leaves a region.
     *
     * @param who player leaving
     * @param region region that was left
     */
    public PlayerLeaveRegionEvent(Player who, Region region) {
        super(who, region);
    }
}
