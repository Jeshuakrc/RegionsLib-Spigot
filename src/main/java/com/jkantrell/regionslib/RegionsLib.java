package com.jkantrell.regionslib;

import com.jkantrell.regionslib.command.RegionCommand;
import com.jkantrell.regionslib.command.commanderProvider.*;
import com.jkantrell.regionslib.command.commanderProvider.annotation.RuleValue;
import com.jkantrell.regionslib.io.Config;
import com.jkantrell.regionslib.persistence.JsonHierarchyRepository;
import com.jkantrell.regionslib.persistence.SQLiteRegionRepository;
import com.jkantrell.regionslib.region.RegionContext;
import com.jkantrell.regionslib.region.ability.Abilities;
import com.jkantrell.regionslib.region.hierarchy.Hierarchy;
import com.jkantrell.regionslib.region.Region;
import com.jkantrell.regionslib.region.hierarchy.HierarchyRepository;
import com.jkantrell.regionslib.region.rule.Rule;
import com.jkantrell.regionslib.region.rule.Rules;
import com.kntrel.mc.commander.command.Commander;
import com.kntrel.mc.commander.command.provider.CommandProvider;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public final class RegionsLib extends JavaPlugin {

    //API
    private static boolean EXISTING_CONTEXT = false;
    @SuppressWarnings("unchecked")
    public static RegionContext newRegionContext(@Nonnull JavaPlugin plugin, @Nonnull URI dataBase, @Nonnull URI hierarchies) {
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


    //FIELDS
    public static final Config CONFIG = new Config("./plugins/regionsLib/config.yml");


    @Override
    public void onEnable() {
        RegionsLib.newRegionContext(this, URI.create("./plugins/RegionsLib/db.sqlite"), URI.create(".plugins/RegionsLib/hierarchies.json"));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
