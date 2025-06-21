package com.kntrel.mc.regionLib.command.commanderProvider;

import com.kntrel.mc.commander.command.Argument;
import com.kntrel.mc.commander.command.provider.CommandProvider;
import com.kntrel.mc.commander.exception.CommandException;
import com.kntrel.mc.commander.exception.CommandUnrunnableException;
import com.kntrel.mc.regionLib.region.RegionContext;
import java.util.List;

public class RuleValueProvider extends CommandProvider<Object> {

    private final RegionContext ctx_;
    private RuleProvider ruleProvider_ = null;
    private CommandProvider<?> valueProvider_ = null;


    public RuleValueProvider(RegionContext ctx) {
        this.ctx_ = ctx;
    }


    @Override
    protected void onInitialization() throws CommandException {
        for (CommandProvider<?> provider : this.getInvocationProviders()) {
            if (provider instanceof RuleProvider ruleProvider) {
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
                    this.valueProvider_ = this.getCommander().getProvider(null, this.ruleProvider_.provide().getValueType().getType()).orElse(null);
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
                this.valueProvider_ = this.getCommander().getProvider(null, this.ruleProvider_.provide().getValueType().getType()).orElse(null);
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
