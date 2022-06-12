package com.jkantrell.regionslib;

import com.jkantrell.commander.command.Commander;
import com.jkantrell.regionslib.commands.RegionCommand;
import com.jkantrell.regionslib.commands.commanderProviders.*;
import com.jkantrell.regionslib.commands.commanderProviders.annotations.RuleValue;
import com.jkantrell.regionslib.io.Config;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.abilities.Abilities;
import com.jkantrell.regionslib.regions.abilities.AbilityHandler;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.rules.RuleDataType;
import com.jkantrell.regionslib.regions.rules.RuleKey;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public final class RegionsLib extends JavaPlugin {

    //FIELDS
    public static boolean enableBuildInAbilities = true;
    public static String[] configLocation = {"./plugins/RegionsLib/config.yml", ""};
    public static final Config CONFIG = new Config("./plugins/regionsLib/config.yml");

    private static JavaPlugin mainInstance_;
    private static AbilityHandler abilityHandler_ = new AbilityHandler();
    private static final HashMap<UUID, PermissionAttachment> permissions_ = new HashMap<>();
    private static LinkedList<Runnable> postEnableTasks_ = new LinkedList<>();
    private static boolean initialized_ = false;

    @Override
    public void onEnable() {
        RegionsLib.enable(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //STATIC METHODS
    public static void enable(JavaPlugin plugin) {

        //Registering the parent plugin. Registers itself if it's running stand-alone.
        RegionsLib.mainInstance_ = plugin;
        RegionsLib.initialized_ = true;

        //Loads the configuration file
        RegionsLib.CONFIG.setFilePath(RegionsLib.configLocation[0]);
        RegionsLib.CONFIG.setSubPath(RegionsLib.configLocation[1]);
        try {
            RegionsLib.CONFIG.load();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //Registering the EventListener class
        plugin.getServer().getPluginManager().registerEvents(new RegionsLibEventListener(), RegionsLib.getMain());
        int sampleRate = RegionsLib.CONFIG.playerSamplingRate;
        if (sampleRate > 0) { Region.setPlayerSampling(sampleRate); }

        //Running post-enabled tasks
        RegionsLib.postEnableTasks_.forEach(Runnable::run);

        //Loading all Hierarchies and regions
        Hierarchy.loadAll();
        if (Region.loadAll().length < 1) {RegionsLib.getMain().getLogger().info("No regions lo load!"); }

        //Registering rules.
        RuleKey.registerNew(RegionsLib.getMain(),"localMod", RuleDataType.BOOL).setAccessPermission("regions.mod");

        //Registering built-in abilities if enabled.
        if (RegionsLib.enableBuildInAbilities) { RegionsLib.getAbilityHandler().registerAll(Abilities.class); }

        //Registering permissions
        Permission permission = new Permission("regions.mod.local");
        RegionsLib.getMain().getServer().getPluginManager().addPermission(permission);

        //Initializing commands
        Commander commander = new Commander(plugin);
        commander.registerProvider(Hierarchy.class,new HierarchyProvider());
        commander.registerProvider(Region.class,new RegionProvider());
        commander.registerProvider(Hierarchy.Group.class,new GroupProvider());
        commander.registerProvider(RuleKey.class,new RuleKeyProvider());
        commander.registerProvider(RuleValue.class, Object.class, new RuleValueProvider());
        commander.register(new RegionCommand());
    }
    public static JavaPlugin getMain() { return mainInstance_; }
    public static AbilityHandler getAbilityHandler() {
        return RegionsLib.abilityHandler_;
    }
    public static void addPostEnableTask(Runnable task) {
        if (RegionsLib.isInitialized()) { task.run(); return; }
        RegionsLib.postEnableTasks_.add(task);
    }
    public static PermissionAttachment getPermissionAttachment(Player target) {
        if (!RegionsLib.permissions_.containsKey(target.getUniqueId())) {
            RegionsLib.permissions_.put(target.getUniqueId(),target.addAttachment(RegionsLib.getMain()));
        }
        return RegionsLib.permissions_.get(target.getUniqueId());
    }
    public static boolean removePermissionAttachment(Player target) {
        return RegionsLib.permissions_.remove(target.getUniqueId()) != null;
    }
    public static boolean isInitialized() {
        return RegionsLib.initialized_;
    }
}
