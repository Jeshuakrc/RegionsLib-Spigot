package com.jkantrell.regionslib.commands.commanderProviders;

import com.jkantrell.commander.command.Argument;
import com.jkantrell.commander.command.provider.CommandProvider;
import com.jkantrell.commander.exception.CommandArgumentException;
import com.jkantrell.commander.exception.CommandException;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.Hierarchy;
import java.util.List;

public class GroupProvider extends CommandProvider<Hierarchy.Group> {

    private Hierarchy.Group group_ = null;
    private RegionProvider regionProvider_ = null;

    private List<Hierarchy.Group> getGroups() {
        try {
            return this.regionProvider_.provide().getHierarchy().getGroups();
        } catch (CommandException | NullPointerException e) {
            return Hierarchy.getAll().stream().map(Hierarchy::getGroups).reduce((a,b) -> { a.addAll(b); return a; }).orElse(null);
        }
    }

    @Override
    protected void onInitialization() throws CommandException {
        for (CommandProvider<?> provider : this.getInvocationProviders()) {
            if (provider instanceof RegionProvider regionProvider) {
                this.regionProvider_ = regionProvider;
                this.regionProvider_.initialize(this);
            }
            if (provider == this) { break; }
        }
    }

    @Override
    public List<String> suggest() {
        List<Hierarchy.Group> groups = this.getGroups();
        return (groups == null) ? null :
                groups.stream().map(Hierarchy.Group::getName).toList();
    }

    @Override
    protected boolean handleArgument(Argument argument) throws CommandException {
        List<Hierarchy.Group> groups = this.getGroups();
        if (groups == null) { throw new CommandArgumentException(argument,"No roles defined yet."); }
        this.group_ = groups.stream().filter(g -> g.getName().equals(argument.getString())).findFirst().orElse(null);
        if (this.group_ == null) { throw new CommandArgumentException(argument,"No such role called '" + argument.getString() + "'."); }
        return true;
    }

    @Override
    public Hierarchy.Group provide() throws CommandException {
        return this.group_;
    }
}
