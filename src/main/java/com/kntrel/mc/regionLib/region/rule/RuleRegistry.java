package com.kntrel.mc.regionLib.region.rule;


import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.react.ReflectiveEventReactorRegistry;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.event.Event;
import java.util.Set;
import java.util.function.Predicate;

public class RuleRegistry extends ReflectiveEventReactorRegistry<Rule<?>> {

    @SuppressWarnings("unchecked")
    public RuleRegistry(RegionContext context) {
        super(context, (Class<Rule<?>>) (Class<?>) Rule.class, DeclareRule.class);
    }

    @Override
    public <E extends Event> RuleBuilder.BooleanRuleBuilder<E> registerOn(Class<E> eventClass) {
        return new InnerBooleanRuleBuilder<>(eventClass, this);
    }

    @Override
    protected void onEvent(Event event, EventKey eventKey) {
        Set<Rule<?>> rules = this.eventMap_.get(eventKey);

        //Keeping the highest priority Ability
        Ability definitive = null;
        for (Rule<?> r : rules) {
            if (!r.appliesTo(event)) { continue; }
            try {
                r.fire(event, this.context_);
            } catch (Throwable t) {
                this.plugin_.getLogger().warning("Error while firing rule '" + r.getName() + "': " + t);
            }
        }

        //TODO: Logging
    }


    protected static class InnerBooleanRuleBuilder<E extends Event> extends RuleBuilder.BooleanRuleBuilder<E> {

        private final RuleRegistry registry_;

        protected InnerBooleanRuleBuilder(Class<E> eventClass, RuleRegistry registry) {
            super(eventClass);
            this.registry_ = registry;
        }

        @Override public <T> RuleBuilder<E, T> having(ValueType<T> type) {
            return new InnerRuleBuilder<>(this, type, registry_);
        }
    }
    protected static class InnerRuleBuilder<E extends Event, T> extends RuleBuilder<E, T> {

        private final RuleRegistry registry_;

        protected InnerRuleBuilder(RuleBuilder<E, ?> other, ValueType<T> type, RuleRegistry registry) {
            super(other, type);
            this.registry_ = registry;
        }

        @Override public Rule<T> build(Predicate<E> additionalChecks) {
            Rule<T> r = super.build(additionalChecks);
            this.registry_.register(r);
            return r;
        }
    }
}
