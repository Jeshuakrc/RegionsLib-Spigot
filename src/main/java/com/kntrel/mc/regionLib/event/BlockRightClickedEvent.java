package com.kntrel.mc.regionLib.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player right-clicks a block with additional context.
 */
public class BlockRightClickedEvent extends BlockEvent implements Cancellable {

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
    /**
     * Returns the handler list for this event instance.
     *
     * @return handler list
     */
    @Override @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }
    //===============================================================

    //FIELDS
    private final Player player_;
    private final ItemStack item_;
    private final BlockFace blockFace_;
    private final EquipmentSlot hand_;
    private final Vector position_;
    private final Location location_;
    private boolean cancelled_ = false;

    //GETTERS
    /**
     * Returns the player who clicked.
     *
     * @return player instance
     */
    public Player getPlayer() {
        return this.player_;
    }
    /**
     * Returns the item held by the player.
     *
     * @return item stack (may be null)
     */
    public ItemStack getItem() {
        return this.item_;
    }
    /**
     * Returns the block that was clicked.
     *
     * @return clicked block
     */
    public Block getClickedBlock() {
        return this.block;
    }
    /**
     * Returns the face of the block that was clicked.
     *
     * @return block face
     */
    public BlockFace getBlockFace() {
        return this.blockFace_;
    }
    /**
     * Returns the hand used to click.
     *
     * @return equipment slot
     */
    public EquipmentSlot getHand() {
        return this.hand_;
    }
    /**
     * Returns the location of the click on the block.
     *
     * @return clicked location
     */
    public Location getClickedLocation() {
        return this.location_;
    }
    /**
     * Returns the relative clicked position on the block.
     *
     * @return relative click vector
     */
    public Vector getClickedPosition() {
        return this.position_;
    }
    /**
     * Returns whether this event is cancelled.
     *
     * @return true if cancelled
     */
    @Override public boolean isCancelled() {
        return cancelled_;
    }


    //SETTERS
    @Override
    /**
     * Sets whether this event is cancelled.
     *
     * @param b true to cancel
     */
    public void setCancelled(boolean b) {
        this.cancelled_ = b;
    }

    /**
     * Creates a block right-click event with the provided context.
     *
     * @param who player who clicked
     * @param item item used
     * @param clickedBlock clicked block
     * @param clickedFace face of the block clicked
     * @param clickedPosition relative position on the block
     * @param hand hand used
     */
    public BlockRightClickedEvent(Player who, ItemStack item, @NotNull Block clickedBlock, BlockFace clickedFace, Vector clickedPosition, EquipmentSlot hand) {
        super(clickedBlock);
        this.player_ = who;
        this.item_ = item;
        this.blockFace_ = clickedFace;
        this.position_ = clickedPosition;
        this.location_ = block.getLocation().add(this.position_);
        this.hand_ = hand;
    }
}
