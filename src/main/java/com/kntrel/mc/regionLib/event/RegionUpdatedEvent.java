package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Fired when a region is updated, exposing the old and new snapshots.
 */
public class RegionUpdatedEvent extends RegionEvent implements Cancellable {

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
    @Override @NotNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final RegionSnapshot old_, current_;
    private final Entity causedBy_;
    private boolean canceled_;


    //CONSTRUCTOR
    /**
     * Creates an update event for a region.
     *
     * @param region updated region
     * @param old previous snapshot
     * @param current current snapshot
     * @param creator entity that triggered the update, if any
     */
    public RegionUpdatedEvent(@NotNull Region region, @NotNull RegionSnapshot old, @NotNull RegionSnapshot current, @Nullable Entity creator) {
        super(region);
        this.old_ = old;
        this.current_ = current;
        this.causedBy_ = creator;
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
     * Returns the entity that triggered the update, if any.
     *
     * @return optional updater
     */
    public Optional<Entity> getUpdater() {
        return Optional.ofNullable(this.causedBy_);
    }
    /**
     * Returns the previous region snapshot.
     *
     * @return old snapshot
     */
    public RegionSnapshot getOldState() {
        return this.old_;
    }
    /**
     * Returns the current region snapshot.
     *
     * @return current snapshot
     */
    public RegionSnapshot getCurrentState() {
        return this.current_;
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


    //UTILITY
    /**
     * Returns true if the region transitioned to destroyed.
     *
     * @return true if destroyed
     */
    public boolean wasDestroyed() {
        return !this.old_.destroyed() && this.current_.destroyed();
    }
    /**
     * Returns true if any bounds changed.
     *
     * @return true if resized
     */
    public boolean wasResized() {
        return     this.old_.minX() != this.current_.minX()
                || this.old_.minY() != this.current_.minY()
                || this.old_.minZ() != this.current_.minZ()
                || this.old_.maxX() != this.current_.maxX()
                || this.old_.maxY() != this.current_.maxY()
                || this.old_.maxZ() != this.current_.maxZ();
    }
    /**
     * Returns true if the region transitioned to disabled.
     *
     * @return true if disabled
     */
    public boolean wasDisabled() {
        return this.old_.enabled() && !this.current_.enabled();
    }
    /**
     * Returns true if the region transitioned to enabled.
     *
     * @return true if enabled
     */
    public boolean wasEnabled() {
        return !this.old_.enabled() && this.current_.enabled();
    }
    /**
     * Returns true if the region name changed.
     *
     * @return true if renamed
     */
    public boolean wasRenamed() {
        return !this.old_.name().equals(this.current_.name());
    }
    /**
     * Returns true if permissions changed.
     *
     * @return true if permissions changed
     */
    public boolean permissionsChanged() {
        return this.old_.permissionsFingerprint() != this.current_.permissionsFingerprint();
    }
    /**
     * Returns true if rule values changed.
     *
     * @return true if rules changed
     */
    public boolean rulesChanged() {
        return this.old_.rulesFingerprint() != this.current_.rulesFingerprint();
    }
    /**
     * Returns true if data container values changed.
     *
     * @return true if data changed
     */
    public boolean dataChanged() {
        return this.old_.dataFingerprint() != this.current_.dataFingerprint();
    }
}
