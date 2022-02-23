package com.jkantrell.regionslib;

import com.jkantrell.regionslib.events.BlockRightClickedEvent;
import com.jkantrell.regionslib.events.CopperBlockInteractEvent;
import com.jkantrell.regionslib.events.LiquidRemoveEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

class RegionsLibEventListener implements Listener {

    @EventHandler
    private void onPlayerInteraction(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getClickedBlock() != null) {
            BlockRightClickedEvent rightClickedEvent = new BlockRightClickedEvent(
                    e.getPlayer(),
                    e.getItem(),
                    e.getClickedBlock(),
                    e.getBlockFace(),
                    e.getHand()
            );
            RegionsLib.getMain().getServer().getPluginManager().callEvent(rightClickedEvent);
            e.setCancelled(rightClickedEvent.isCancelled());
        }

        ItemStack item = e.getItem();
        if (item == null) { return; }
        if (!item.getType().equals(Material.BUCKET)) { return; }

        Block liquid = null;
        boolean found = false;
        LiquidRemoveEvent.Type type = null;
        for (Block block : e.getPlayer().getLineOfSight(null,15)) {
            found = true;
            switch (block.getType()) {
                case LAVA -> type = LiquidRemoveEvent.Type.LAVA;
                case WATER -> {
                    type = LiquidRemoveEvent.Type.WATER;
                    int i = 0;
                    BlockFace[] faces = {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
                    for (BlockFace f : faces) {
                        Block b = block.getRelative(f);
                        if (b.getType().equals(Material.WATER)) {
                            if (((Levelled) b.getBlockData()).getLevel() == 0) { i++; }
                        }
                        if (i >= 2) {
                            type = LiquidRemoveEvent.Type.INFINITE_WATER;
                            break;
                        }
                    }
                }
                default -> found = false;
            }
            if (found) { liquid = block; break; }
        }
        if (!found) { return; }

        LiquidRemoveEvent liquidRemoveEvent = new LiquidRemoveEvent(e.getPlayer(),liquid,type);
        RegionsLib.getMain().getServer().getPluginManager().callEvent(liquidRemoveEvent);
        e.setCancelled(liquidRemoveEvent.isCancelled());
    }

    @EventHandler
    private void onRightClickBlock(BlockRightClickedEvent e) {
         Block block = e.getBlock();
         Material type = block.getType();
         ItemStack item = e.getItem();
         if (type.toString().contains("COPPER") && !type.equals(Material.RAW_COPPER_BLOCK) && item != null) {
             CopperBlockInteractEvent.Action action =   (item.getType().equals(Material.HONEYCOMB)) ? CopperBlockInteractEvent.Action.WAX :
                                                        (item.getType().toString().contains("AXE")) ? CopperBlockInteractEvent.Action.SCRAP : null;
             if (action != null) {
                 CopperBlockInteractEvent event = new CopperBlockInteractEvent(e.getPlayer(),block,action);
                 RegionsLib.getMain().getServer().getPluginManager().callEvent(event);
                 e.setCancelled(event.isCancelled());
             }
         }
    }
}