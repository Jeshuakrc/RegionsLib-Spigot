package com.jkantrell.regionslib;

import com.jkantrell.regionslib.events.*;
import com.jkantrell.regionslib.regions.Region;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RegionsLibEventListener implements Listener {

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

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        RegionsLib.removePermissionAttachment(e.getPlayer());
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

        List<String> perms = RegionsLibEventListener.permissionsMap_.stream()
                .filter(e -> e.getKey().equals(player.getName()))
                .map(Map.Entry::getValue)
                .distinct()
                .toList();

        PermissionAttachment attachment = RegionsLib.getPermissionAttachment(player);

        attachment.getPermissions().entrySet().stream()
                .filter(e -> !perms.contains(e.getKey()) && e.getValue())
                .map(Map.Entry::getKey)
                .forEach(p -> {
                    attachment.unsetPermission(p);
                    RegionsLib.getMain().getLogger().fine("Removed \"" + p + "\" permission from " + player.getName() + ".");
                });

        Map<String,Boolean> newPermission = attachment.getPermissions();

        perms.stream()
                .filter(p -> {
                    if (!newPermission.containsKey(p)) { return true; }
                    return !newPermission.get(p);
                })
                .forEach(p -> {
                    attachment.setPermission(p,true);
                    RegionsLib.getMain().getLogger().fine("Granted \"" + p + "\" to " + player.getName() + ".");
                });
    }

}
