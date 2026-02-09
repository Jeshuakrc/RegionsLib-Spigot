package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player enters a region.
 */
public class PlayerEnterRegionEvent extends PlayerEvent {
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

    //FIELDS
    private final Region region_;
    private final Location location_;

    //CONSTRUCTORS
    /**
     * Creates an event when a player enters a region.
     *
     * @param who player entering
     * @param region region that was entered
     */
    public PlayerEnterRegionEvent(Player who, Region region) {
        super(who);
        this.location_ = who.getLocation();
        this.region_ = region;

    }

    //GETTERS
    /**
     * Returns the region the player entered.
     *
     * @return entered region
     */
    public Region getRegion() {
        return this.region_;
    }
    /**
     * Returns the location where the player entered.
     *
     * @return entry location
     */
    public Location getLocation() {
        return this.location_;
    }

}
