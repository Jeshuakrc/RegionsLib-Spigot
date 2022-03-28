package com.jkantrell.regionslib;

import com.jkantrell.regionslib.io.Config;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.abilities.Abilities;
import com.jkantrell.regionslib.regions.abilities.AbilityHandler;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.FileNotFoundException;

public final class RegionsLib extends JavaPlugin {

    //FIELDS
    public static boolean enableBuildInAbilities = true;
    public static String[] configLocation = {"./plugins/RegionsLib/config.yml", ""};
    public static final Config CONFIG = new Config("./plugins/regionsLib/config.yml");

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

        //Registering the parent plugin. Registers itself if it's running stand-alone.
        RegionsLib.registerPlugin(plugin);

        //Loads the configuration file
        RegionsLib.CONFIG.setFilePath(RegionsLib.configLocation[0]);
        RegionsLib.CONFIG.setSubPath(RegionsLib.configLocation[1]);
        try {
            RegionsLib.CONFIG.load();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //Loading all Hierarchies and regions
        Hierarchy.loadAll();
        if (Region.loadAll().isEmpty()) {RegionsLib.getMain().getLogger().info("No regions lo load!"); }

        //Registering the EventListener class
        plugin.getServer().getPluginManager().registerEvents(new RegionsLibEventListener(), RegionsLib.getMain());

        //Registering built-in abilities if enabled.
        if (RegionsLib.enableBuildInAbilities) { RegionsLib.getAbilityHandler().registerAll(Abilities.class); }
    }

    public static Plugin getMain() { return mainInstance_; }
    public static AbilityHandler getAbilityHandler() {
        return RegionsLib.abilityHandler_;
    }

    public static void registerPlugin(Plugin plugin) { RegionsLib.mainInstance_ = plugin; }
}
