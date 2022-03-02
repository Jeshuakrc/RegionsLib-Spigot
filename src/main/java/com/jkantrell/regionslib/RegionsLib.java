package com.jkantrell.regionslib;

import com.jkantrell.regionslib.io.ConfigManager;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.abilities.Abilities;
import com.jkantrell.regionslib.regions.abilities.AbilityHandler;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionsLib extends JavaPlugin {

    //FIELDS
    public static boolean enableBuildInAbilities = true;

    private static Plugin mainInstance_;
    private static AbilityHandler abilityHandler_ = new AbilityHandler();

    @Override
    public void onEnable() {
        RegionsLib.enable(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static void enable(Plugin plugin) {
        RegionsLib.registerPlugin(plugin);
        ConfigManager.initialize();
        Hierarchy.loadAll();
        plugin.getServer().getPluginManager().registerEvents(new RegionsLibEventListener(), RegionsLib.getMain());
        RegionsLib.getAbilityHandler().registerAll(Abilities.class);
        if (Region.loadAll().isEmpty()) {RegionsLib.getMain().getLogger().info("No regions lo load!"); }
    }

    public static Plugin getMain() { return mainInstance_; }
    public static AbilityHandler getAbilityHandler() {
        return RegionsLib.abilityHandler_;
    }

    public static void registerPlugin(Plugin plugin) { RegionsLib.mainInstance_ = plugin; }
}
