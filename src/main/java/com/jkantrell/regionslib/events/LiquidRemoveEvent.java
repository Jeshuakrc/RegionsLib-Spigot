package com.jkantrell.regionslib.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

import javax.annotation.Nonnull;

public class LiquidRemoveEvent extends BlockEvent implements Cancellable {

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

    //ENUMS
    public enum Type {
        LAVA, WATER, INFINITE_WATER;
    }

    //FIELDS
    private final LiquidRemoveEvent.Type type_;
    private final Player player_;
    private boolean cancelled_ = false;

    //SETTERS
    @Override
    public void setCancelled(boolean b) {
        this.cancelled_ = b;
    }

    //GETTERS
    public LiquidRemoveEvent.Type getType() {
        return this.type_;
    }
    public Player getPlayer() {
        return this.player_;
    }
    @Override
    public boolean isCancelled() {
        return cancelled_;
    }

    //CONSTRUCTOR
    public LiquidRemoveEvent(Player player, Block theBlock, LiquidRemoveEvent.Type type) {
        super(theBlock);
        this.player_ = player;
        this.type_ = type;
    }
}
