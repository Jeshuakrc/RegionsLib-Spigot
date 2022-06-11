package com.jkantrell.regionslib.commands.commanderProviders;

import com.jkantrell.commander.command.Argument;
import com.jkantrell.commander.command.provider.CommandProvider;
import com.jkantrell.commander.exception.CommandException;
import com.jkantrell.commander.exception.CommandUnrunnableException;

import java.util.List;

public class RuleValueProvider extends CommandProvider<Object> {

    private RuleKeyProvider ruleProvider_ = null;
    private CommandProvider<?> valueProvider_ = null;

    @Override
    protected void onInitialization() throws CommandException {
        for (CommandProvider<?> provider : this.getInvocationProviders()) {
            if (provider instanceof RuleKeyProvider ruleProvider) {
                this.ruleProvider_ = ruleProvider;
            }
            if (provider == this) { break; }
        }
        if (ruleProvider_ == null) {
            this.getCommander().getLogger().severe(
                "Unable to execute command. A RegionRule parameter is required before the Value parameter."
            );
            throw new CommandUnrunnableException("Unable to run this command");
        }
    }
    @Override
    public List<String> suggest() {
        if (this.valueProvider_ == null) {
            if (this.getSupplyConsecutive() < 1) {
                try {
                    this.valueProvider_ = this.getCommander().getProvider(null, this.ruleProvider_.provide().getDataType().getClazz());
                } catch (CommandException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return valueProvider_.suggest();
    }
    @Override
    protected boolean handleArgument(Argument argument) throws CommandException {
        if (this.valueProvider_ == null) {
            if (this.getSupplyConsecutive() < 2) {
                this.valueProvider_ = this.getCommander().getProvider(null, this.ruleProvider_.provide().getDataType().getClazz());
                this.valueProvider_.initialize(this);
            } else {
                throw new CommandUnrunnableException("Unknown rule.");
            }
        }
        return this.valueProvider_.supply(argument);
    }

    @Override
    public Object provide() throws CommandException {
        return this.valueProvider_.provide();
    }
}
