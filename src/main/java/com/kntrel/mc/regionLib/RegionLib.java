package com.kntrel.mc.regionLib;

import com.kntrel.mc.commvoker.argument.ArgumentRegistry;
import com.kntrel.mc.commvoker.argument.binder.ArgumentBinder;
import com.kntrel.mc.commvoker.spigot.Commvoker;
import com.kntrel.mc.regionLib.command.RegionCommand;
import com.kntrel.mc.regionLib.command.assembler.*;
import com.kntrel.mc.regionLib.persistence.sqlite.JsonHierarchyRepository;
import com.kntrel.mc.regionLib.persistence.sqlite.SQLiteRegionRepository;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.provided.Abilities;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.mc.regionLib.provided.Rules;
import com.kntrel.mc.regionLib.util.Grid;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class RegionLib extends JavaPlugin {
    // -------------------- REGION CONTEXT REGISTRY ----------------------- //
    private static final Map<String, RegionContext> REGISTRY = new HashMap<>();
    private static RegionContext DEFAULT = null;

    public static List<RegionContext> getContexts() {
        return List.copyOf(REGISTRY.values());
    }
    public static RegionContext getContext(String name) {
        return REGISTRY.get(name);
    }
    public static RegionContext getDefaultContext() {
        return DEFAULT;
    }
    public static void publish(RegionContext context) {
        REGISTRY.put(context.getNamespace(), context);
    }
    public static void setDefault(RegionContext context) {
        publish(context);
        DEFAULT = context;
    }


    // -------------------- SHADED MODE API ------------------------------- //
    private static final RegionContextConfig DEFAULT_CONFIG = new RegionContextConfig(4, 32, Permission.OverlapMode.OLDEST, 10, Grid.CellSize.SIZE_32, 1024);
    private static Plugin OWNER_PLUGIN = null;

    private static void ensureEnabled() {
        if (OWNER_PLUGIN == null) {
            throw new IllegalStateException("RegionLib must be enabled before use");
        }
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void extractResource(String resourcePath, File parent) {
        parent.mkdirs();
        File out = new File(parent, resourcePath);
        try (var in = RegionLib.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing embedded resource: " + resourcePath);
            }
            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (FileAlreadyExistsException ignored) {
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract " + resourcePath + " to " + out, e);
        }
    }
    public static void enable(@NotNull JavaPlugin plugin) {

        //Handle already enabled
        if (OWNER_PLUGIN != null) {
            if (OWNER_PLUGIN == plugin) { return; }

            String msg = "RegionLib has already been enabled";
            if (!(OWNER_PLUGIN instanceof RegionLib)) {
                msg += " and it's owned by the '" + OWNER_PLUGIN.getName() + "' plugin";
            }
            throw new IllegalStateException(msg);
        }

        //Enabling
        OWNER_PLUGIN = plugin;

        //Initialising internal events singleton
        EventTriggerer.enable(OWNER_PLUGIN);
        OWNER_PLUGIN.getServer().getPluginManager().registerEvents(new RegionLibEventListener(plugin), plugin);


        //Registering the /region command
        Commvoker commvoker = new Commvoker(plugin);
        ArgumentRegistry<CommandSender> argumentRegistry = commvoker.getArgumentRegistry();
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(RegionAssembler::region)
                        .toClass(Region.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(HierarchyAssembler::hierarchy)
                        .toClass(Hierarchy.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(HierarchyGroupAssembler::hierarchyGroup)
                        .toClass(Hierarchy.Group.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(RuleAssembler::rule)
                        .toClass((Class<Rule<?>>) (Class<?>) Rule.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(RuleValueAssembler::ruleValue)
                        .toClass((Class<RuleValue<?>>) (Class<?>) RuleValue.class)
                        .bind()
        );
        commvoker.register(new RegionCommand());

        wireSlf4jToPluginLogger("com.kntrel", plugin);
    }
    public static void tryEnable(@NotNull Plugin plugin) {
        try { enable((JavaPlugin) plugin); } catch (IllegalStateException ignored) {}
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static RegionContext createContext(Plugin plugin, String namespace, RegionContextConfig config, URI hierarchies, URI database) {
        ensureEnabled();

        File dbFile = new File(database);
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            try { dbFile.createNewFile(); } catch (IOException e) { throw new RuntimeException(e); }
        }
        File hierarchiesFile = new File(hierarchies);
        if (!hierarchiesFile.exists()) {
            hierarchiesFile.getParentFile().mkdirs();
            try { hierarchiesFile.createNewFile(); } catch (IOException e) { throw new RuntimeException(e); }
        }

        RegionContext out = new RegionContext(
                namespace,
                config,
                plugin,
                ctx -> new SQLiteRegionRepository(plugin, ctx, database),
                ctx -> new JsonHierarchyRepository(hierarchiesFile)
        );
        out.getAbilityRegistry().registerFrom(Abilities.class);
        out.getRuleRegistry().registerFrom(Rules.class);

        return out;
    }
    public static RegionContext createContext(Plugin plugin, URI hierarchies, URI database) {
        return createContext(plugin, plugin.getName(), DEFAULT_CONFIG, hierarchies, database);
    }
    public static RegionContext createDefaultContext(Plugin plugin) {
        URI database = new File(plugin.getDataFolder(), ".db").toURI();
        File hierarchiesFile = new File(plugin.getDataFolder(), "hierarchies.json");
        if (!hierarchiesFile.exists()) {
            extractResource("hierarchies.json", plugin.getDataFolder());
        }
        RegionContext context = createContext(plugin, hierarchiesFile.toURI(), database);
        setDefault(context);
        return context;
    }


    // -------------------- PLUGIN MODE IMPLEMENTATION -------------------- //
    @Override public void onEnable() {
        RegionLib.enable(this);
        RegionLib.createDefaultContext(this);
    }

    @Override
    public void onDisable() {}


    // ------------------------------- HELPERS --------------------------- //
    private static void wireSlf4jToPluginLogger(String rootPackage, Plugin plugin) {
        var pluginLogger = plugin.getLogger();
        var pkgLogger = java.util.logging.Logger.getLogger(rootPackage);

        pkgLogger.setParent(pluginLogger);
        pkgLogger.setUseParentHandlers(true);

        pluginLogger.setLevel(Level.ALL);
        pkgLogger.setLevel(Level.ALL);
    }
}
