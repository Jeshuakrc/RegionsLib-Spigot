package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RegionUpdatedEvent extends RegionEvent implements Cancellable {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NotNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final RegionSnapshot old_, current_;
    private final Entity causedBy_;
    private boolean canceled_;


    //CONSTRUCTOR
    public RegionUpdatedEvent(@NotNull Region region, @NotNull RegionSnapshot old, @NotNull RegionSnapshot current, @Nullable Entity creator) {
        super(region);
        this.old_ = old;
        this.current_ = current;
        this.causedBy_ = creator;
        this.canceled_ = false;
    }


    //GETTERS
    @Override public boolean isCancelled() {
        return this.canceled_;
    }
    public Optional<Entity> getUpdater() {
        return Optional.ofNullable(this.causedBy_);
    }
    public RegionSnapshot getOldState() {
        return this.old_;
    }
    public RegionSnapshot getCurrentState() {
        return this.current_;
    }


    //SETTERS
    @Override public void setCancelled(boolean b) {
        this.canceled_ = b;
    }


    //UTILITY
    public boolean wasDestroyed() {
        return !this.old_.destroyed() && this.current_.destroyed();
    }
    public boolean wasResized() {
        return     this.old_.minX() != this.current_.minX()
                || this.old_.minY() != this.current_.minY()
                || this.old_.minZ() != this.current_.minZ()
                || this.old_.maxX() != this.current_.maxX()
                || this.old_.maxY() != this.current_.maxY()
                || this.old_.maxZ() != this.current_.maxZ();
    }
    public boolean wasDisabled() {
        return this.old_.enabled() && !this.current_.enabled();
    }
    public boolean wasEnabled() {
        return !this.old_.enabled() && this.current_.enabled();
    }
    public boolean wasRenamed() {
        return !this.old_.name().equals(this.current_.name());
    }
    public boolean permissionsChanged() {
        return this.old_.permissionsFingerprint() != this.current_.permissionsFingerprint();
    }
    public boolean rulesChanged() {
        return this.old_.rulesFingerprint() != this.current_.rulesFingerprint();
    }
    public boolean dataChanged() {
        return this.old_.dataFingerprint() != this.current_.dataFingerprint();
    }
}
