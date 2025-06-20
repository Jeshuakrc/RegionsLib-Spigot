package com.jkantrell.regionslib;

import com.jkantrell.regionslib.command.RegionCommand;
import com.jkantrell.regionslib.command.commanderProvider.*;
import com.jkantrell.regionslib.command.commanderProvider.annotation.RuleValue;
import com.jkantrell.regionslib.io.Config;
import com.jkantrell.regionslib.region.hierarchy.Hierarchy;
import com.jkantrell.regionslib.region.Region;
import com.kntrel.mc.commander.command.Commander;
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
    public static final Config CONFIG = new Config("./plugins/regionsLib/config.yml");


    @Override
    public void onEnable() {
        RegionsLib.enable(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //STATIC METHODS
    public static void enable(JavaPlugin plugin) {}

}
