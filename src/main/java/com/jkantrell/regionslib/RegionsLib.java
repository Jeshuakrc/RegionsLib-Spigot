package com.jkantrell.regionslib;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionsLib extends JavaPlugin {

    private static Plugin mainInstance_ = getPlugin(RegionsLib.class);

    @Override
    public void onEnable() {
        // Plugin startup logic
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Plugin getMain() { return mainInstance_; }
    public static void setMain(Plugin plugin) { RegionsLib.mainInstance_ = plugin; }
}
