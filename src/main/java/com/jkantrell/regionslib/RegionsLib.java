package com.jkantrell.regionslib;

import com.jkantrell.regionslib.abilities.AbilityHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionsLib extends JavaPlugin {

    //FIELDS
    private static Plugin mainInstance_ = getPlugin(RegionsLib.class);
    private static AbilityHandler abilityHandler_ = new AbilityHandler();

    @Override
    public void onEnable() {
        // Plugin startup logic
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Plugin getMain() { return mainInstance_; }
    public static AbilityHandler getAbilityHandler() {
        return RegionsLib.abilityHandler_;
    }

    public static void registerPlugin(Plugin plugin) { RegionsLib.mainInstance_ = plugin; }
}
