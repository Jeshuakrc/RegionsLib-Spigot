package com.kntrel.mc.regionLib.command;

import com.kntrel.mc.commvoker.bukkit.CommandResult;
import com.kntrel.mc.commvoker.bukkit.provided.annotation.Sender;
import com.kntrel.mc.commvoker.bukkit.requirement.RequiresPermission;
import com.kntrel.mc.commvoker.command.Command;
import com.kntrel.mc.commvoker.error.FailTrigger;
import com.kntrel.mc.commvoker.exception.FailedCommandException;
import com.kntrel.mc.regionLib.region.NamespaceRegionKey;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import java.util.*;

@Command("region")
public class RegionCommand {


    //COMMANDS
    @Command("create {area} in {world} {hierarchy} {name}")
    @RequiresPermission("regions.create")
    public String create(FailTrigger ft, CommandSender sender, BoundingBox boundingBox, World world, Hierarchy hierarchy, String name) throws FailedCommandException {
        NamespaceRegionKey rk = NamespaceRegionKey.of(name);
        try {
            Region region = new Region(
                    rk.getRegionContext(),
                    boundingBox,
                    world,
                    rk.name(),
                    hierarchy,
                    (sender instanceof Entity e) ? e : null
            );
            region.save();
        } catch (IllegalArgumentException e) {
            ft.fail(e.getMessage());
        }
        return  "Region '" + name + "' created successfully!";
    }

    @Command("create {area} {hierarchy} {name}")
    @RequiresPermission("regions.create")
    public String create(FailTrigger ft, @Sender Player player, BoundingBox boundingBox, Hierarchy hierarchy, String name) throws FailedCommandException {
        return create(ft, player, boundingBox, player.getWorld(), hierarchy, name);
    }

    @Command("destroy {region}")
    @RequiresPermission("regions.destroy")
    public String destroy(CommandSender sender, List<Region> regions) {
        if (regions.isEmpty()) { return ""; }

        final Entity destroyer = (sender instanceof Entity) ? (Entity) sender : null;
        regions.forEach(r -> { r.destroy(destroyer); r.save(); });

        StringBuilder msg = new StringBuilder();
        if (regions.size() < 2) {
            msg.append("Region ").append(regions.getFirst().getName()).append(" has ");
        } else {
            msg.append("Regions ");
            for (int i = 0; i < regions.size(); i++) {
                if (i == (regions.size() - 1)) {
                    msg.append("and ");
                }
                msg.append(regions.get(i).getName()).append(" ");
            }
            msg.append("have ");
        }
        msg.append("been destroyed");

        return msg.toString();
    }

    @Command("resize {region} {new_area}")
    @RequiresPermission("regions.resize")
    public String resize(Region region, BoundingBox newArea) {
        region.resize(newArea);
        region.save();
        return region.getName() + " has been resized. New dimensions: [" + region.getWidthX() + " x " + region.getHeight() + " x " + region.getWidthZ() + "].";
    }

    @Command("expand {region} {direction} {how_much}")
    @RequiresPermission("regions.resize")
    public String expand(Region region, BlockFace direction, Double howMuch) {
        region.expand(direction, howMuch);
        region.save();
        return region.getName() + " has been resized. New dimensions: [" + region.getWidthX() + " x " + region.getHeight() + " x " + region.getWidthZ() + "].";
    }

    @Command("rename {region} {new_name}")
    @RequiresPermission("regions.mod.local")
    public String rename(CommandSender sender, Region region, String name) {
        String oldName = region.getName();
        region.setName(name);
        region.save();
        return oldName + "'s name has been changed to \"" + name + "\".";
    }

    @Command("tp {entities} to {region}")
    @RequiresPermission("regions.command.tp-to")
    public void tpPlayerTo(List<Entity> entities, Region region) {
        Vector center = region.getBoundingBox().getCenter();
        World world = region.getWorld();
        double x = center.getX(), z = center.getZ(), y = world.getHighestBlockYAt((int) x,(int) z) + 1;
        entities.forEach(e -> e.teleport(new Location(world,x,y,z), PlayerTeleportEvent.TeleportCause.COMMAND));
    }

    @Command("tp to {region}")
    @RequiresPermission("regions.command.tp-to")
    public void tpTo(@Sender Player sender, Region region) {
        this.tpPlayerTo(List.of(sender), region);
    }


    @Command("join {players} to {region} as {group}")
    @RequiresPermission("regions.mod.local")
    public CommandResult playerJoin(CommandSender sender, List<Player> players, Region region, Hierarchy.Group group) {
        if (players.isEmpty()) { return CommandResult.success(); }

        Map<Player, String> msgs = new HashMap<>(players.size());

        String individualMsg =
                (sender instanceof Player p ? (p.getName() + " added you") : "You've been added")
                + " to " + region.getName() + " as " + group.getName();
        StringBuilder msg = new StringBuilder();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            region.addPermission(p, group);

            if (i == (players.size() - 1) && i > 0) {
                msg.append("and ");
            }
            msg.append(p.getName()).append(" ");
            msgs.put(p, individualMsg);
        }
        msg.append((players.size() > 1) ? "have" : "has")
           .append(" benn added to ")
           .append(region.getName())
           .append(" as ")
           .append(group.getName());

        region.save();
        CommandResult res = CommandResult.success(msg.toString());
        res.setPlayerMessageStrings(msgs);
        return res;
    }

    @Command("kick {players} from {region}")
    @RequiresPermission("regions.mod.local")
    public CommandResult playerKick(List<Player> players, Region region) {
        if (players.isEmpty()) { return CommandResult.success(); }

        String individualMsg = "You've been kicked out of " + region.getName();
        StringBuilder msg = new StringBuilder();
        Map<Player, String> msgs = new HashMap<>(players.size());

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (region.removePermissions(p)) { msgs.put(p, individualMsg); }
            if (i == (players.size() - 1) && i > 0) {
                msg.append("and ");
            }
            msg.append(p.getName()).append(" ");
        }
        msg.append((players.size() > 1) ? "have" : "has")
                .append(" benn kicked out from ")
                .append(region.getName());

        region.save();
        CommandResult res = CommandResult.success(msg.toString());
        res.setPlayerMessageStrings(msgs);
        return res;
    }

    @Command("set {ruleValue} in {regions}")
    @RequiresPermission("regions.mod.local")
    public String setRule(RuleValue<?> ruleValue, List<Region> regions) {
        if (regions.isEmpty()) { return ""; }

        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            r.setRuleValue(ruleValue);
            if (i == (regions.size() - 1) && i > 0) {
                msg.append("and ");
            }
            msg.append(r.getName()).append(" ");
        }
        msg.append((regions.size() > 1) ? "have" : "has")
                .append(" benn updated: '")
                .append(ruleValue.getRule().getName())
                .append("' set to '")
                .append(ruleValue)
                .append("'");

        regions.forEach(Region::save);
        return msg.toString();
    }

    @Command("set default {rule} in {regions}")
    @RequiresPermission("regions.mod.local")
    public String setRuleDefault(Rule<?> rule, List<Region> regions) {
        if (regions.isEmpty()) { return ""; }

        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            r.removeRule(rule.getName());
            if (i == (regions.size() - 1) && i > 0) {
                msg.append("and ");
            }
            msg.append(r.getName()).append(" ");
        }
        msg.append((regions.size() > 1) ? "have" : "has")
                .append(" benn updated: '")
                .append(rule.getName())
                .append("' set to default");

        regions.forEach(Region::save);
        return msg.toString();
    }

    @Command("view {region}")
    @RequiresPermission("regions.command.showlimit")
    public void showLimit(@Sender Player player, Region region) {
        region.display(player);
    }

    @Command("enable {regions}")
    @RequiresPermission("regions.onoff")
    public List<String> enable(List<Region> regions) {
        if (regions.isEmpty()) { return Collections.emptyList(); }

        StringBuilder msg = new StringBuilder();
        List<Region> alreadyEnabled = new ArrayList<>();
        List<String> out = new ArrayList<>(2);

        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            if (r.isEnabled()) {
                alreadyEnabled.add(r);
                continue;
            }
            r.enable();
            if (i == (regions.size() - 1) && i > 0) {
                msg.append("and ");
            }
            msg.append(r.getName()).append(" ");
        }

        if (!msg.isEmpty()) {
            msg.append((regions.size() > 1) ? "have" : "has")
                    .append(" benn enabled");
            out.add(msg.toString());
        }

        if (!alreadyEnabled.isEmpty()) {
            msg.setLength(0);
            for (int i = 0; i < alreadyEnabled.size(); i++) {
                Region r = alreadyEnabled.get(i);
                if (i == (alreadyEnabled.size() - 1) && i > 0) {
                    msg.append("and ");
                }
                msg.append(r.getName()).append(" ");
            }
            msg.append((alreadyEnabled.size() > 1) ? "were" : "was")
                    .append(" already enabled");
            out.add(msg.toString());
        }

        return out;
    }

    @Command("disable {regions}")
    @RequiresPermission("regions.onoff")
    public List<String> disable(List<Region> regions) {
        if (regions.isEmpty()) { return Collections.emptyList(); }

        StringBuilder msg = new StringBuilder();
        List<Region> alreadyDisabled = new ArrayList<>();
        List<String> out = new ArrayList<>(2);

        for (int i = 0; i < regions.size(); i++) {
            Region r = regions.get(i);
            if (!r.isEnabled()) {
                alreadyDisabled.add(r);
                continue;
            }
            r.disable();
            if (i == (regions.size() - 1) && i > 0) {
                msg.append("and ");
            }
            msg.append(r.getName()).append(" ");
        }

        if (!msg.isEmpty()) {
            msg.append((regions.size() > 1) ? "have" : "has")
                    .append(" benn disabled");
            out.add(msg.toString());
        }

        if (!alreadyDisabled.isEmpty()) {
            msg.setLength(0);
            for (int i = 0; i < alreadyDisabled.size(); i++) {
                Region r = alreadyDisabled.get(i);
                if (i == (alreadyDisabled.size() - 1) && i > 0) {
                    msg.append("and ");
                }
                msg.append(r.getName()).append(" ");
            }
            msg.append((alreadyDisabled.size() > 1) ? "were" : "was")
                    .append(" already disabled");
            out.add(msg.toString());
        }

        return out;
    }
}
