package com.kntrel.mc.regionLib.event;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

class EventTriggerer implements Listener {

    //SINGLETON
    private static Plugin PLUGIN = null;
    static void enable(@NotNull Plugin plugin) {
        if (plugin.equals(PLUGIN)) { return; }
        if (PLUGIN != null) {
            throw new IllegalStateException("RegionLIb's event triggerer is already owned by the '" + PLUGIN.getName() + "' plugin.");
        }

        PLUGIN = plugin;
        PLUGIN.getServer().getPluginManager().registerEvents(new EventTriggerer(), PLUGIN);
    }


    //TRIGGERERS
    @EventHandler
    private void triggerBlockRightClickedEvent(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) { return; }
        if (e.getClickedBlock() == null) { return; }

        BlockRightClickedEvent rightClickedEvent = new BlockRightClickedEvent(
                e.getPlayer(),
                e.getItem(),
                e.getClickedBlock(),
                e.getBlockFace(),
                e.getClickedPosition(),
                e.getHand()
        );
        trigger(rightClickedEvent);
        e.setCancelled(rightClickedEvent.isCancelled());
    }

    @EventHandler
    private void triggerCopperBlockInteractEvent(BlockRightClickedEvent e) {
        Block block = e.getBlock();
        Material material = block.getType();
        if (Tag.COPPER_ORES.isTagged(material)) { return; }
        if (!material.toString().contains("COPPER")) { return; }

        ItemStack item = e.getItem();
        if (item == null) { return; }

        CopperBlockInteractEvent.Action action = null;
        if (item.getType().equals(Material.HONEYCOMB)) {
            action = CopperBlockInteractEvent.Action.WAX;
        } else if (Tag.ITEMS_AXES.isTagged(item.getType())) {
            action = CopperBlockInteractEvent.Action.SCRAP;
        }
        if (action == null) { return; }

        CopperBlockInteractEvent event = new CopperBlockInteractEvent(e.getPlayer(), block, action);
        trigger(event);
        e.setCancelled(event.isCancelled());
    }


    //HELPERS
    private static void trigger(Event e) {
        PLUGIN.getServer().getPluginManager().callEvent(e);
    }
}
