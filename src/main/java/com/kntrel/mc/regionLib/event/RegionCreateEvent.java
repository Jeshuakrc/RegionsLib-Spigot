package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

/**
 * Fired when a region is created.
 */
public class RegionCreateEvent extends RegionEvent implements Cancellable {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * Returns the static handler list for this event.
     *
     * @return handler list
     */
    public static HandlerList getHandlerList() { return HANDLERS; }
    /**
     * Returns the handler list for this event instance.
     *
     * @return handler list
     */
    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final Entity creator_;
    private boolean canceled_;


    //CONSTRUCTOR
    /**
     * Creates a region creation event.
     *
     * @param region created region
     * @param creator entity that created the region, if any
     */
    public RegionCreateEvent(@NotNull Region region, @Nullable Entity creator) {
        super(region);
        this.creator_ = creator;
        this.canceled_ = false;
    }


    //GETTERS
    /**
     * Returns whether the event is cancelled.
     *
     * @return true if cancelled
     */
    @Override public boolean isCancelled() {
        return this.canceled_;
    }
    /**
     * Returns the entity that created the region, if available.
     *
     * @return optional creator
     */
    public Optional<Entity> getCreator() {
        return Optional.ofNullable(this.creator_);
    }


    //SETTERS
    /**
     * Sets whether the event is cancelled.
     *
     * @param b true to cancel
     */
    @Override public void setCancelled(boolean b) {
        this.canceled_ = b;
    }
}
