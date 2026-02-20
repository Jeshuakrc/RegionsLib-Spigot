package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.Chunk;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RegionLoadEvent extends RegionEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NotNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final Chunk chunk_;


    //CONSTRUCTORS
    public RegionLoadEvent(@NotNull Region region, Chunk byChunk) {
        super(region);
        this.chunk_ = byChunk;
    }


    //GETTERS
    private Chunk byChunk() { return this.chunk_; }
}
