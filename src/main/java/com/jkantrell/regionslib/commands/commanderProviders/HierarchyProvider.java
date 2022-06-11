package com.jkantrell.regionslib.commands.commanderProviders;

import com.jkantrell.commander.command.Argument;
import com.jkantrell.commander.exception.CommandArgumentException;
import com.jkantrell.commander.exception.CommandException;
import com.jkantrell.commander.command.provider.CommandProvider;
import com.jkantrell.regionslib.regions.Hierarchy;

import java.util.List;
import java.util.stream.Collectors;

public class HierarchyProvider extends CommandProvider<Hierarchy> {

    //FIELD
    private Hierarchy hierarchy_;

    //OVERWRITES
    @Override
    public List<String> suggest() {
        return Hierarchy.getAll().stream()
                .map(Hierarchy::getName)
                .collect(Collectors.toList());
    }

    @Override
    protected boolean handleArgument(Argument argument) throws CommandException {
        if (argument.isInt()) {
            this.hierarchy_ = Hierarchy.get(argument.getInt());
        } else {
            this.hierarchy_ = Hierarchy.get(argument.getString());
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
