package com.jkantrell.regionslib.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

import javax.annotation.Nonnull;

public class CopperBlockInteractEvent extends BlockEvent implements Cancellable {
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
    public enum Stage {
        NORMAL, EXPOSED, WEATHERED, OXIDIZED
    }
    public enum Type {
        NORMAL, CUT, CUT_STAIRS, CUT_SLAB
    }
    public enum Action {
        NONE, WAX, SCRAP
    }

    //FIELDS
    private final Player player_;
    private final CopperBlockInteractEvent.Action action_;
    private final CopperBlockInteractEvent.Type type_;
    private final CopperBlockInteractEvent.Stage stage_;
    private final boolean waxed_;
    private boolean cancelled_;

    //SETTERS
    public void setCancelled(boolean cancelled) {
        this.cancelled_ = cancelled;
    }

    //GETTERS
    public boolean wasWaxed() {
        return this.waxed_;
    }
    public Action getAction() {
        return action_;
    }
    public Type getType() {
        return type_;
    }
    public Stage getStage() {
        return stage_;
    }
    public Player getPlayer() {
        return player_;
    }
    @Override
    public boolean isCancelled() {
        return cancelled_;
    }

    //CONSTRUCTOR
    public CopperBlockInteractEvent(Player who, Block copperBlock, Action action) {
        super(copperBlock);
        this.player_ = who;

        String material = copperBlock.getType().toString();
        if (!material.contains("COPPER")) { throw new IllegalArgumentException("Not a copper block"); }

        this.waxed_ = material.contains("WAXED");

        if (material.contains("CUT")) {
            if (material.contains("STAIRS")) {
                this.type_ = Type.CUT_STAIRS;
            } else if (material.contains("SLAB")) {
                this.type_ = Type.CUT_SLAB;
            } else {
                this.type_ = Type.CUT;
            }
        } else {
            this.type_ = Type.NORMAL;
        }

        if (material.contains("EXPOSED")) {
            this.stage_ = Stage.EXPOSED;
        } else if (material.contains("WEATHERED")) {
            this.stage_ = Stage.WEATHERED;
        } else if (material.contains("OXIDIZED")) {
            this.stage_ = Stage.OXIDIZED;
        } else {
            this.stage_ = Stage.NORMAL;
        }

        this.action_ = switch (action) {
            case NONE -> Action.NONE;
            case WAX -> (this.wasWaxed()) ? Action.NONE : Action.WAX;
            case SCRAP -> (this.wasWaxed()) ? Action.SCRAP : Action.NONE;
        };
    }
}
