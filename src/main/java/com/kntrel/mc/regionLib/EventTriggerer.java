package com.kntrel.mc.regionLib;

import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.regionLib.event.CopperBlockInteractEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

class EventTriggerer implements Listener {

    //SINGLETON
    private static EventTriggerer INSTANCE = null;
    static void enable(@NotNull Plugin plugin) {
        if (INSTANCE != null) {
            if (INSTANCE.plugin_ == plugin) { return; }
            HandlerList.unregisterAll(INSTANCE);
        }

        INSTANCE = new EventTriggerer(plugin);
        plugin.getServer().getPluginManager().registerEvents(INSTANCE, plugin);
    }
    static void disable() {
        if (INSTANCE != null) {
            HandlerList.unregisterAll(INSTANCE);
            INSTANCE = null;
        }
    }

    //FIELDS
    private final Plugin plugin_;


    //CONSTRUCTORS
    private EventTriggerer(Plugin plugin) {
        this.plugin_ = plugin;
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

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin_) {
            disable();
        }
    }


    //HELPERS
    private void trigger(Event e) {
        this.plugin_.getServer().getPluginManager().callEvent(e);
    }
}
