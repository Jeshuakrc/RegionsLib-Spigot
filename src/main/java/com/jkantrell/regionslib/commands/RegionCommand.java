package com.jkantrell.regionslib.commands;

import com.jkantrell.commander.CommandHolder;
import com.jkantrell.commander.command.Command;
import com.jkantrell.commander.provider.identify.ExcludeWorld;
import com.jkantrell.commander.provider.identify.Sender;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.Permission;
import com.jkantrell.regionslib.regions.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Command(label = "region")
public class RegionCommand extends CommandHolder {

    @Command(label = "create")
    public boolean create(@Sender Player player, @ExcludeWorld Location vertex1, @ExcludeWorld Location vertex2, Hierarchy hierarchy, String name) {
        return create(vertex1,vertex2,player.getWorld(),hierarchy,name);
    }

    @Command(label = "create")
    public boolean create(@ExcludeWorld Location vertex1, @ExcludeWorld Location vertex2, World world, Hierarchy hierarchy, String name) {
        Region region = new Region(
                new double[] {vertex1.getX(),vertex1.getY(),vertex1.getZ(),vertex2.getX(),vertex2.getY(),vertex2.getZ()},
                world,
                new Permission[0],
                name,
                hierarchy
        );
        region.save();

        return true;
    }

    @Command(label = "delete")
    public boolean delete(Region region) {
        region.destroy();
        return true;
    }

}
