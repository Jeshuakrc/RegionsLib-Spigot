package com.kntrel.mc.regionLib;

import com.kntrel.mc.regionLib.command.RegionCommand;
import com.kntrel.mc.regionLib.command.commanderProvider.annotation.RuleValue;
import com.kntrel.mc.regionLib.command.commanderProvider.*;
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
import org.bukkit.plugin.java.JavaPlugin;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public final class RegionLib extends JavaPlugin {

    //API
    private static boolean EXISTING_CONTEXT = false;
    @SuppressWarnings("unchecked")
    public static RegionContext newRegionContext(@Nonnull JavaPlugin plugin, @Nonnull URI dataBase, @Nonnull File hierarchies) {
        if (EXISTING_CONTEXT) {
            throw new IllegalStateException("An instance of RegionContext has already been provided.");
        }

        RegionContext rc = new RegionContext(
            plugin,
            ctx -> new SQLiteRegionRepository(
                plugin,
                ctx,
                dataBase,
                new JsonHierarchyRepository(hierarchies)
            )
        );

        Commander commander = new Commander(plugin);
        commander.registerProvider(Hierarchy.Group.class, () -> new GroupProvider(rc));
        commander.registerProvider(Hierarchy.class, () -> new HierarchyProvider(rc));
        commander.registerProvider(Region.class, () -> new RegionProvider(rc));
        commander.registerProvider(Rule.class, () -> (CommandProvider<Rule>) (CommandProvider<?>) new RuleProvider(rc));
        commander.registerProvider(RuleValue.class, Object.class, () -> new RuleValueProvider(rc));
        commander.register(new RegionCommand(rc));

        rc.getAbilityRegistry().registerFrom(Abilities.class);
        rc.getRuleRegistry().registerFrom(Rules.class);

        EXISTING_CONTEXT = true;
        return rc;
    }
    public static RegionContext newRegionContext(@Nonnull JavaPlugin plugin) {

        File dbFile = new File(plugin.getDataFolder(), ".db");
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            try { dbFile.createNewFile(); } catch (IOException e) { throw new RuntimeException(e); }
        }

        File hierarchiesFile = new File(plugin.getDataFolder(), "hierarchies.json");
        if (!hierarchiesFile.exists()) {
            plugin.saveResource("hierarchies.json",true);
        }

        return RegionLib.newRegionContext(plugin, dbFile.toURI(), hierarchiesFile);
    }


    //FIELDS
    public static final Config CONFIG = new Config("./plugins/regionsLib/config.yml");


    @Override
    public void onEnable() {
        RegionLib.newRegionContext(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
