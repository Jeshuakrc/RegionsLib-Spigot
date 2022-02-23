package com.jkantrell.regionslib.events;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
    @Override
    public boolean isCancelled() {
        return cancelled_;
    }

    //SETTERS
    @Override
    public void setCancelled(boolean b) {
        this.cancelled_ = b;
    }

    public BlockRightClickedEvent(Player who, ItemStack item, @Nonnull Block clickedBlock, BlockFace clickedFace, EquipmentSlot hand) {
        super(clickedBlock);
        this.player_ = who;
        this.item_ = item;
        this.blockFace_ = clickedFace;
        this.hand_ = hand;
    }
}
