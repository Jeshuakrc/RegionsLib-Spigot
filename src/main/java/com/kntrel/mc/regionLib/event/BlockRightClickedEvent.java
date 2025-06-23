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

import javax.annotation.Nonnull;

public class BlockRightClickedEvent extends BlockEvent implements Cancellable {

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

    //FIELDS
    private final Player player_;
    private final ItemStack item_;
    private final BlockFace blockFace_;
    private final EquipmentSlot hand_;
    private final Vector position_;
    private final Location location_;
    private boolean cancelled_ = false;

    //GETTERS
    public Player getPlayer() {
        return this.player_;
    }
    public ItemStack getItem() {
        return this.item_;
    }
    public Block getClickedBlock() {
        return this.block;
    }
    public BlockFace getBlockFace() {
        return this.blockFace_;
    }
    public EquipmentSlot getHand() {
        return this.hand_;
    }
    public Location getClickedLocation() {
        return this.location_;
    }
    public Vector getClickedPosition() {
        return this.position_;
    }
    @Override public boolean isCancelled() {
        return cancelled_;
    }


    //SETTERS
    @Override
    public void setCancelled(boolean b) {
        this.cancelled_ = b;
    }

    public BlockRightClickedEvent(Player who, ItemStack item, @Nonnull Block clickedBlock, BlockFace clickedFace, Vector clickedPosition, EquipmentSlot hand) {
        super(clickedBlock);
        this.player_ = who;
        this.item_ = item;
        this.blockFace_ = clickedFace;
        this.position_ = clickedPosition;
        this.location_ = block.getLocation().add(this.position_);
        this.hand_ = hand;
    }
}
