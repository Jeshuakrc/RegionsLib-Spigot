package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public class RegionCreateEvent extends RegionEvent implements Cancellable {
    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final Entity creator_;
    private boolean canceled_;


    //CONSTRUCTOR
    public RegionCreateEvent(Region region, @Nullable Entity creator) {
        super(region);
        this.creator_ = creator;
        this.canceled_ = false;
    }

    //GETTERS
    @Override public boolean isCancelled() {
        return this.canceled_;
    }
    public Optional<Entity> getCreator() {
        return Optional.ofNullable(this.creator_);
    }

    //SETTERS
    @Override public void setCancelled(boolean b) {
        this.canceled_ = b;
    }
}
