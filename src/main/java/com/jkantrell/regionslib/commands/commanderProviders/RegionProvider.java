package com.jkantrell.regionslib.commands.commanderProviders;

import com.jkantrell.commander.command.Argument;
import com.jkantrell.commander.exception.CommandArgumentException;
import com.jkantrell.commander.exception.CommandException;
import com.jkantrell.commander.command.provider.CommandProvider;
import com.jkantrell.regionslib.regions.Region;
import java.util.List;
import java.util.stream.Collectors;

public class RegionProvider extends CommandProvider<Region> {

    Region region_;
    CommandArgumentException multipleRegionsException_ = null;

    @Override
    public List<String> suggest() {
        return Region.getAll().stream()
                .map(region -> {
                    String name = region.getName();
                    return (name.contains(" ") ? "\"" + name + "\"" : name);
                })
                .collect(Collectors.toList());
    }

    @Override
    protected boolean handleArgument(Argument argument) throws CommandException {
        if (argument.isInt()) {
            Region region = Region.get(argument.getInt());
            if (region == null) {
                throw new CommandArgumentException(argument, "There's no region under the ID " + argument.getInt() + ".");
            }
            this.region_ = region;
            return true;
        }
        Region[] regions = Region.get(argument.getString());
        if (regions.length < 1) {
            throw new CommandArgumentException(argument, "There's no region under the name '" + argument.getString() + "'.");
        } else if (regions.length > 1) {
            StringBuilder builder = new StringBuilder();
            builder.append("Regions under IDs ");
            for (int i = 0; i < regions.length; i++) {
                builder.append(regions[i].getId());
                if ((i + 2) < regions.length) {
                    builder.append(", ");
                } else if ((i + 1) < regions.length) {
                    builder.append(" and ");
                }
            }
            builder.append(" are under the name '").append(argument.getString()).append("'. Use their ID instead.");

            this.multipleRegionsException_ = new CommandArgumentException(argument, builder.toString());
        }
        this.region_ = regions[0];

        return true;
    }

    @Override
    public Region provide() throws CommandException {
        if (this.multipleRegionsException_ != null) {
            throw this.multipleRegionsException_;
        }
        return region_;
    }
}
