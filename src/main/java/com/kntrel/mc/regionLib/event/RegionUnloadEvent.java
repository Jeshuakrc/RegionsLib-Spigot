package com.kntrel.mc.regionLib.event;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.Chunk;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RegionUnloadEvent extends RegionEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NotNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final Chunk chunk_;


    //CONSTRUCTORS
    public RegionUnloadEvent(@NotNull Region region, Chunk withChunk) {
        super(region);
        this.chunk_ = withChunk;
    }


    //GETTERS
    private Chunk withChunk() { return this.chunk_; }

}
