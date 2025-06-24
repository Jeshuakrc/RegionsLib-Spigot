package com.kntrel.mc.regionLib;

import com.kntrel.mc.regionLib.command.RegionCommand;
import com.kntrel.mc.regionLib.command.commanderProvider.annotation.RuleValue;
import com.kntrel.mc.regionLib.command.commanderProvider.*;
import com.kntrel.mc.regionLib.event.RegionLibEvent;
import com.kntrel.mc.regionLib.io.Config;
import com.kntrel.mc.regionLib.persistence.JsonHierarchyRepository;
import com.kntrel.mc.regionLib.persistence.SQLiteRegionRepository;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.ability.Abilities;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.Rules;
import com.kntrel.mc.commander.command.Commander;
import com.kntrel.mc.commander.command.provider.CommandProvider;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;

public final class RegionLib extends JavaPlugin {


    //API
    private static Plugin OWNER_PLUGIN = null;


    public static RegionContext enable(@Nonnull JavaPlugin plugin, @Nonnull URI dataBase, @Nonnull File hierarchies) {

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
        Commander commander = new Commander(plugin);
        commander.registerProvider(Hierarchy.Group.class, () -> new GroupProvider(rc));
        commander.registerProvider(Hierarchy.class, () -> new HierarchyProvider(rc));
        commander.registerProvider(Region.class, () -> new RegionProvider(rc));
        commander.registerProvider(Rule.class, () -> (CommandProvider<Rule>) (CommandProvider<?>) new RuleProvider(rc));
        commander.registerProvider(RuleValue.class, Object.class, () -> new RuleValueProvider(rc));
        commander.register(new RegionCommand(rc));

        //Setting up custom logging handler
        java.util.logging.Logger pluginLogger = plugin.getLogger();
        pluginLogger.setLevel(Level.ALL);
        pluginLogger.addHandler(new RegionLibLogHandler(plugin.getName()));
        pluginLogger.setUseParentHandlers(false);


        return rc;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static RegionContext enable(@Nonnull JavaPlugin plugin) {

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

    public static RegionContext newRegionContext(@Nonnull JavaPlugin plugin, @Nonnull URI dataBase, @Nonnull File hierarchies) {

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
    public static final Config CONFIG = new Config("./plugins/regionsLib/config.yml");


    @Override public void onEnable() {
        RegionLib.enable(this);
    }

    @Override
    public void onDisable() {}

}
