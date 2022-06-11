package com.jkantrell.regionslib.commands.commanderProviders;

import com.jkantrell.commander.command.Argument;
import com.jkantrell.commander.command.provider.CommandProvider;
import com.jkantrell.commander.exception.CommandArgumentException;
import com.jkantrell.commander.exception.CommandException;
import com.jkantrell.regionslib.regions.rules.RuleKey;

import java.util.List;

public class RuleKeyProvider extends CommandProvider<RuleKey> {

    private RuleKey key_;

    @Override
    public List<String> suggest() {
        return RuleKey.getAll().stream().filter(k -> k.testPermission(this.getCommandSender())).map(RuleKey::getLabel).toList();
    }

    @Override
    protected boolean handleArgument(Argument argument) throws CommandException {
        this.key_ = RuleKey.get(argument.getString());
        if (this.key_ == null) {
            throw new CommandArgumentException(argument,"Not a valid rule.");
        } if (!this.key_.testPermission(this.getCommandSender())) {
            throw new CommandArgumentException(argument,"You're not allowed to alter this rule.");
        }
        return true;
    }

    @Override
    public RuleKey provide() throws CommandException {
        return this.key_;
    }
}
