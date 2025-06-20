package com.jkantrell.regionslib.command.commanderProvider;

import com.jkantrell.regionslib.region.RegionContext;
import com.jkantrell.regionslib.region.rule.Rule;
import com.kntrel.mc.commander.command.Argument;
import com.kntrel.mc.commander.command.provider.CommandProvider;
import com.kntrel.mc.commander.exception.CommandArgumentException;
import com.kntrel.mc.commander.exception.CommandException;
import java.util.List;

public class RuleProvider extends CommandProvider<Rule<?>> {

    private final RegionContext ctx_;
    private Rule<?> key_;

    public RuleProvider(RegionContext ctx) {
        this.ctx_ = ctx;
    }

    @Override
    public List<String> suggest() {
        return this.ctx_.getRuleRegistry().getAll().stream().map(Rule::getName).toList();
    }

    @Override
    protected boolean handleArgument(Argument argument) throws CommandException {
        this.key_ = this.ctx_.getRuleRegistry().get(argument.getString()).orElse(null);
        if (this.key_ == null) {
            throw new CommandArgumentException(argument, "Not a valid rule.");
        }
        return true;
    }

    @Override
    public Rule<?> provide() throws CommandException {
        return this.key_;
    }
}
