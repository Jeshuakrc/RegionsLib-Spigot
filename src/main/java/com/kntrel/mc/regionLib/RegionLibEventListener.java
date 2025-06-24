package com.kntrel.mc.regionLib;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import java.util.*;

class RegionLibEventListener implements Listener {

    private final Plugin plugin_;

    public RegionLibEventListener(Plugin plugin) {
        this.plugin_ = plugin;
    }


    //FIELDS
    private static final List<Map.Entry<String,String>> permissionsMap_ = new LinkedList<>();

    //LISTENERS




    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        RegionLibEventListener.setPermissions_(e.getPlayer());
    }

    //STATIC METHODS
    public static void addPermissionRegistration(String playerName, String permission) {
        RegionLibEventListener.permissionsMap_.add(Map.entry(playerName,permission));

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) { return; }
        RegionLibEventListener.setPermissions_(player);
    }
    public static void removePermissionRegistration(String playerName, String permission) {
        RegionLibEventListener.permissionsMap_.stream()
                .filter(e -> e.getKey().equals(playerName) && e.getValue().equals(permission))
                .findFirst()
                .ifPresent(RegionLibEventListener.permissionsMap_::remove);
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) { return; }
        RegionLibEventListener.setPermissions_(player);
    }

    private static void setPermissions_(Player player) {


    }

}
