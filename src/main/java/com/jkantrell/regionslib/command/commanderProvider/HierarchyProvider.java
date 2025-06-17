package com.jkantrell.regionslib.command.commanderProvider;

import com.jkantrell.regionslib.region.RegionContext;
import com.jkantrell.regionslib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.commander.command.Argument;
import com.kntrel.mc.commander.command.provider.CommandProvider;
import com.kntrel.mc.commander.exception.CommandArgumentException;
import com.kntrel.mc.commander.exception.CommandException;
import com.jkantrell.regionslib.region.hierarchy.Hierarchy;
import java.util.List;
import java.util.stream.Collectors;

public class HierarchyProvider extends CommandProvider<Hierarchy> {

    //FIELD
    private final RegionContext ctx_;
    private final HierarchyRepository repo_;
    private Hierarchy hierarchy_;

    //CONSTRUCTOR
    public HierarchyProvider(RegionContext ctx) {
        this.ctx_ = ctx;
        this.repo_ = this.ctx_.getHierarchyRepository();
    }

    //OVERWRITES
    @Override
    public List<String> suggest() {
        return this.repo_.getAll().stream()
                .map(Hierarchy::getName)
                .collect(Collectors.toList());
    }

    @Override
    protected boolean handleArgument(Argument argument) throws CommandException {
        if (argument.isInt()) {
            this.hierarchy_ = this.repo_.get(Long.valueOf(argument.getInt())).orElse(null);
        } else {
            List<Hierarchy> list = this.repo_.getByName(argument.getString());
            this.hierarchy_ = list.isEmpty() ? null : list.get(0);
        }
        if (hierarchy_ == null) {
            throw new CommandArgumentException(argument,"No such hierarchy found under under ID/Name " + argument.getString() + ".");
        }

        return true;
    }

    @Override
    public Hierarchy provide() throws CommandException {
        return this.hierarchy_;
    }
}
