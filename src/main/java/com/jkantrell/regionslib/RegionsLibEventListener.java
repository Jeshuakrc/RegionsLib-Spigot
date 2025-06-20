package com.jkantrell.regionslib;

import com.jkantrell.regionslib.event.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class RegionsLibEventListener implements Listener {

    private final Plugin plugin_;

    public RegionsLibEventListener(Plugin plugin) {
        this.plugin_ = plugin;
    }


    //FIELDS
    private static final List<Map.Entry<String,String>> permissionsMap_ = new LinkedList<>();

    //LISTENERS
    @EventHandler(ignoreCancelled = true)
    private void onPlayerInteraction(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getClickedBlock() != null) {
            BlockRightClickedEvent rightClickedEvent = new BlockRightClickedEvent(
                    e.getPlayer(),
                    e.getItem(),
                    e.getClickedBlock(),
                    e.getBlockFace(),
                    e.getHand()
            );
            this.plugin_.getServer().getPluginManager().callEvent(rightClickedEvent);
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
        this.plugin_.getServer().getPluginManager().callEvent(liquidRemoveEvent);
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
                 this.plugin_.getServer().getPluginManager().callEvent(event);
                 e.setCancelled(event.isCancelled());
             }
         }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        RegionsLibEventListener.setPermissions_(e.getPlayer());
    }

    //STATIC METHODS
    public static void addPermissionRegistration(String playerName, String permission) {
        RegionsLibEventListener.permissionsMap_.add(Map.entry(playerName,permission));

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) { return; }
        RegionsLibEventListener.setPermissions_(player);
    }
    public static void removePermissionRegistration(String playerName, String permission) {
        RegionsLibEventListener.permissionsMap_.stream()
                .filter(e -> e.getKey().equals(playerName) && e.getValue().equals(permission))
                .findFirst()
                .ifPresent(RegionsLibEventListener.permissionsMap_::remove);
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) { return; }
        RegionsLibEventListener.setPermissions_(player);
    }

    private static void setPermissions_(Player player) {


    }

}
