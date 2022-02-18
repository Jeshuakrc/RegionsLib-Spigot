package com.jkantrell.regionslib;

import com.jkantrell.regionslib.regions.abilities.Ability;
import com.jkantrell.regionslib.regions.abilities.AbilityHandler;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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

        Region[] regions = Region.getAt(1,1,1, getServer().getWorld(""));
        List<Ability<?>> abilities = (List<Ability<?>>) regions[0].getHierarchy().getGroup(1).getAbilities();
    }

    public static Plugin getMain() { return mainInstance_; }
    public static AbilityHandler getAbilityHandler() {
        return RegionsLib.abilityHandler_;
    }

    public static void registerPlugin(Plugin plugin) { RegionsLib.mainInstance_ = plugin; }
}
