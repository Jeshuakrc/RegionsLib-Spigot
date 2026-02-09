package com.kntrel.mc.regionLib.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player interacts with a copper block and wax/scrape actions are evaluated.
 */
public class CopperBlockInteractEvent extends BlockEvent implements Cancellable {
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

    //ENUMS
    /**
     * Oxidation stage for copper blocks.
     */
    public enum Stage {
        NORMAL, EXPOSED, WEATHERED, OXIDIZED
    }
    /**
     * Copper block variant.
     */
    public enum Type {
        NORMAL, CUT, CUT_STAIRS, CUT_SLAB
    }
    /**
     * The interaction action taken.
     */
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
    /**
     * Sets whether this event is cancelled.
     *
     * @param cancelled true to cancel
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled_ = cancelled;
    }

    //GETTERS
    /**
     * Returns whether the copper block was waxed before the interaction.
     *
     * @return true if waxed
     */
    public boolean wasWaxed() {
        return this.waxed_;
    }
    /**
     * Returns the resolved interaction action.
     *
     * @return action enum
     */
    public Action getAction() {
        return action_;
    }
    /**
     * Returns the copper block variant.
     *
     * @return copper type
     */
    public Type getType() {
        return type_;
    }
    /**
     * Returns the oxidation stage.
     *
     * @return oxidation stage
     */
    public Stage getStage() {
        return stage_;
    }
    /**
     * Returns the player who interacted.
     *
     * @return player instance
     */
    public Player getPlayer() {
        return player_;
    }
    /**
     * Returns whether this event is cancelled.
     *
     * @return true if cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled_;
    }

    //CONSTRUCTOR
    /**
     * Creates a copper interaction event.
     *
     * @param who player who interacted
     * @param copperBlock copper block being interacted with
     * @param action requested action
     */
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
