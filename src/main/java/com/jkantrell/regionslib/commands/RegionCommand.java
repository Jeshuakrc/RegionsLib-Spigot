package com.jkantrell.regionslib.commands;

import com.jkantrell.commander.command.CommandHolder;
import com.jkantrell.commander.command.annotations.Command;
import com.jkantrell.commander.command.annotations.Requires;
import com.jkantrell.commander.command.provider.identify.ExcludeWorld;
import com.jkantrell.commander.command.provider.identify.Sender;
import com.jkantrell.commander.exception.CommandUnrunnableException;
import com.jkantrell.regionslib.commands.commanderProviders.annotations.RuleValue;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.rules.Rule;
import com.jkantrell.regionslib.regions.rules.RuleKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

@Command(label = "region")
@Requires(permission = "regions.mod.local")
public class RegionCommand extends CommandHolder {

    @Command(label = "create")
    @Requires(permission = "regions.create")
    public boolean create(@Sender Player player, @ExcludeWorld Location vertex1, @ExcludeWorld Location vertex2, Hierarchy hierarchy, String name) throws CommandUnrunnableException {
        return create(player, vertex1, vertex2, player.getWorld(), hierarchy, name);
    }

    @Command(label = "create")
    @Requires(permission = "regions.create")
    public boolean create(CommandSender sender, @ExcludeWorld Location vertex1, @ExcludeWorld Location vertex2, World world, Hierarchy hierarchy, String name) throws CommandUnrunnableException {
        try {
            Region region = new Region(
                    new double[]{vertex1.getX(), vertex1.getY(), vertex1.getZ(), vertex2.getX(), vertex2.getY(), vertex2.getZ()},
                    world,
                    name,
                    hierarchy,
                    (sender instanceof Entity) ? (Entity) sender : null
            );
            if (region.isDestroyed()) { return false; }
            region.save();
        } catch (IllegalArgumentException e) {
            throw new CommandUnrunnableException(e.getMessage());
        }
        sender.sendMessage("Region '" + name + "' created successfully!");
        return true;
    }

    @Command(label = "destroy")
    @Requires(permission = "regions.destroy")
    public boolean destroy(CommandSender sender, Region region) {
        region.destroy((sender instanceof Entity) ? (Entity) sender : null);
        sender.sendMessage(region.getName() + " was successfully deleted.");
        return true;
    }

    @Command(label = "rename")
    @Requires(permission = "regions.mod.local")
    public boolean rename(CommandSender sender, Region region, String name) throws CommandUnrunnableException {
        String oldName = region.getName();
        try {
            region.setName(name);
        } catch (IllegalArgumentException e) {
            throw new CommandUnrunnableException(e.getMessage());
        }
        region.save();
        sender.sendMessage(oldName + "'s name has been changed to '" + name + "'.");
        return true;
    }

    @Command(label = "tp-to")
    @Requires(permission = "regions.command.tp-to")
    public boolean tpTo(@Sender Player player, Region region) {
        Vector center = region.getBoundingBox().getCenter();
        World world = player.getWorld();
        double x = center.getX(); double z = center.getZ();
        double y = player.getWorld().getHighestBlockYAt((int) x,(int) z) + 1;
        player.teleport(new Location(world,x,y,z), PlayerTeleportEvent.TeleportCause.COMMAND);
        return true;
    }

    @Command(label = "tp-to")
    @Requires(permission = "regions.command.tp-to")
    public boolean tpTo(Region region, Player player) {
        Vector center = region.getBoundingBox().getCenter();
        World world = player.getWorld();
        double x = center.getX(); double z = center.getZ();
        double y = player.getWorld().getHighestBlockYAt((int) x,(int) z) + 1;
        player.teleport(new Location(world,x,y,z), PlayerTeleportEvent.TeleportCause.COMMAND);
        return true;
    }

    @Command(label = "player join")
    @Requires(permission = "regions.mod.local")
    public boolean playerJoin(CommandSender sender, Region region, Player player, Hierarchy.Group group) throws CommandUnrunnableException {
        try {
            region.addPermission(player,group);
        } catch (IllegalArgumentException e) {
            throw new CommandUnrunnableException(e.getMessage());
        }
        region.save();
        sender.sendMessage(player.getName() + " has been added into " + region.getName() + " with the " + group.getName() + " role.");
        return true;
    }

    @Command( label = "player kick")
    @Requires(permission = "regions.mod.local")
    public boolean playerKick(CommandSender sender, Region region, Player player) {
        boolean r = region.removePermissions(player);
        sender.sendMessage(
            r ? player.getName() + " is no longer a member of " + region.getName() + "."
            : player.getName() + " had no role in " + region.getName() + ". No changes made."
        );
        if (r) { region.save(); }
        return r;
    }

    @Command( label = "setrule" )
    @Requires(permission = "regions.mod.local")
    public boolean setRule(CommandSender sender, Region region, RuleKey ruleKey, @RuleValue Object value) throws CommandUnrunnableException {
        if (!ruleKey.testPermission(sender)) {
            throw new CommandUnrunnableException("You're not allowed to alter this rule.");
        }
        try {
            Rule rule;
            if (!region.hasRule(ruleKey)) {
                rule = new Rule(ruleKey, value);
                region.addRule(rule);
            } else {
                rule = region.getRule(ruleKey.getLabel());
                rule.set(value);
            }
        } catch (IllegalArgumentException e) {
            throw new CommandUnrunnableException("invalid rule value provided.");
        }
        region.save();
        sender.sendMessage(
            "The value of '" + ruleKey.getLabel() + "' has been set to '" + value.toString() + "'."
        );
        return true;
    }

    @Command( label = "setrule default" )
    @Requires(permission = "regions.mod.local")
    public boolean setRuleDefault(CommandSender sender, Region region, RuleKey ruleKey) {
        boolean removed = region.removeRule(ruleKey.getLabel());
        region.save();
        sender.sendMessage(removed ?
                "Rule '" + ruleKey.getLabel() + "' has been reset." :
                "Rule '" + ruleKey.getLabel() + "' was unaltered. Nothing changed."
            );
        return removed;
    }


}
