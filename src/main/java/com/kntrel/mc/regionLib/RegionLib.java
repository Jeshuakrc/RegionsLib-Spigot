package com.kntrel.mc.regionLib;

import com.kntrel.mc.commvoker.argument.ArgumentRegistry;
import com.kntrel.mc.commvoker.argument.binder.ArgumentBinder;
import com.kntrel.mc.commvoker.spigot.Commvoker;
import com.kntrel.mc.regionLib.command.RegionCommand;
import com.kntrel.mc.regionLib.command.assembler.*;
import com.kntrel.mc.regionLib.event.RegionLibEvent;
import com.kntrel.mc.regionLib.io.Config;
import com.kntrel.mc.regionLib.persistence.JsonHierarchyRepository;
import com.kntrel.mc.regionLib.persistence.SQLiteRegionRepository;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.ability.Abilities;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.mc.regionLib.region.rule.Rules;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;

public final class RegionLib extends JavaPlugin {


    //API
    private static Plugin OWNER_PLUGIN = null;


    public static RegionContext enable(@NotNull JavaPlugin plugin, @NotNull URI dataBase, @NotNull File hierarchies) {

        //Handle already enabled
        if (OWNER_PLUGIN != null) {
            String msg = "RegionLib has already been enabled";
            if (!(OWNER_PLUGIN instanceof RegionLib)) {
                msg += " and it's owned by the '" + OWNER_PLUGIN.getName() + "' plugin";
            }
            throw new IllegalStateException(msg);
        }

        //Enabling
        OWNER_PLUGIN = plugin;

        //Initialising internal events singleton
        RegionLibEvent.enable(OWNER_PLUGIN);
        OWNER_PLUGIN.getServer().getPluginManager().registerEvents(new RegionLibEventListener(plugin), plugin);

        //Getting the main RegionContext
        RegionContext rc = RegionLib.newRegionContext(plugin, dataBase, hierarchies);

        //Registering the /region command based on the main RegionContext
        Commvoker commvoker = new Commvoker(plugin);
        ArgumentRegistry<CommandSender> argumentRegistry = commvoker.getArgumentRegistry();
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(() -> RegionAssembler.regionFromRepository(rc))
                        .toClass(Region.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(() -> HierarchyAssembler.hierarchyFromRegistry(rc.getHierarchyRepository()))
                        .toClass(Hierarchy.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(HierarchyGroupAssembler::hierarchyGroup)
                        .toClass(Hierarchy.Group.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(() -> RuleAssembler.ruleFromRegistry(rc.getRuleRegistry()))
                        .toClass((Class<Rule<?>>) (Class<?>) Rule.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(() -> RuleValueAssembler.ruleFromRegistry(rc.getRuleRegistry()))
                        .toClass((Class<RuleValue<?>>) (Class<?>) RuleValue.class)
                        .bind()
        );

        commvoker.register(new RegionCommand(rc));

        //Setting up custom logging handler
        java.util.logging.Logger pluginLogger = plugin.getLogger();
        pluginLogger.setLevel(Level.ALL);
        pluginLogger.addHandler(new RegionLibLogHandler(plugin.getName()));
        pluginLogger.setUseParentHandlers(false);


        return rc;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static RegionContext enable(@NotNull JavaPlugin plugin) {

        File dbFile = new File(plugin.getDataFolder(), ".db");
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            try { dbFile.createNewFile(); } catch (IOException e) { throw new RuntimeException(e); }
        }

        File hierarchiesFile = new File(plugin.getDataFolder(), "hierarchies.json");
        if (!hierarchiesFile.exists()) {
            plugin.saveResource("hierarchies.json",true);
        }

        return RegionLib.enable(plugin, dbFile.toURI(), hierarchiesFile);
    }

    public static RegionContext newRegionContext(@NotNull JavaPlugin plugin, @NotNull URI dataBase, @NotNull File hierarchies) {

        RegionContext rc = new RegionContext(
            plugin,
            ctx -> new SQLiteRegionRepository(
                plugin,
                ctx,
                dataBase,
                new JsonHierarchyRepository(hierarchies)
            )
        );

        rc.getAbilityRegistry().registerFrom(Abilities.class);
        rc.getRuleRegistry().registerFrom(Rules.class);

        return rc;
    }

    //FIELDS
    public static final Config CONFIG = new Config();


    @Override public void onEnable() {
        RegionLib.enable(this);
    }

    @Override
    public void onDisable() {}

}
