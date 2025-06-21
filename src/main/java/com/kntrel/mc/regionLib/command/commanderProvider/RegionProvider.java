package com.kntrel.mc.regionLib.command.commanderProvider;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.commander.command.Argument;
import com.kntrel.mc.commander.command.provider.CommandProvider;
import com.kntrel.mc.commander.exception.CommandArgumentException;
import com.kntrel.mc.commander.exception.CommandException;

import java.util.List;
import java.util.stream.Collectors;

public class RegionProvider extends CommandProvider<Region> {

    private final RegionContext ctx_;
    Region region_;
    CommandArgumentException multipleRegionsException_ = null;

    public RegionProvider(RegionContext ctx) {
        this.ctx_ = ctx;
    }


    @Override
    public List<String> suggest() {
        return this.ctx_.getAll().stream()
                .map(region -> {
                    String name = region.getName();
                    return (name.contains(" ") ? "\"" + name + "\"" : name);
                })
                .collect(Collectors.toList());
    }

    @Override
    protected boolean handleArgument(Argument argument) throws CommandException {
        if (argument.isInt()) {
            this.region_ = this.ctx_.get((long) argument.getInt())
                    .orElseThrow(() -> new CommandArgumentException(argument, "There's no region under the ID " + argument.getInt() + "."));
            return true;
        }
        List<Region> regions = this.ctx_.get(argument.getString());
        if (regions.isEmpty()) {
            throw new CommandArgumentException(argument, "There's no region under the name '" + argument.getString() + "'.");
        } else if (regions.size() > 1) {
            StringBuilder builder = new StringBuilder();
            builder.append("Regions under IDs ");
            for (int i = 0; i < regions.size(); i++) {
                builder.append(regions.get(i).getId());
                if ((i + 2) < regions.size()) {
                    builder.append(", ");
                } else if ((i + 1) < regions.size()) {
                    builder.append(" and ");
                }
            }
            builder.append(" are under the name '").append(argument.getString()).append("'. Use their ID instead.");

            this.multipleRegionsException_ = new CommandArgumentException(argument, builder.toString());
        }
        this.region_ = regions.get(0);

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
