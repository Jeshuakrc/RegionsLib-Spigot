package com.kntrel.mc.regionLib.region.rule;


import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.react.ReflectiveEventReactorRegistry;
import com.kntrel.mc.regionLib.trigger.TriggerListenerRegistry;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class RuleRegistry extends TriggerListenerRegistry<RuleTrigger<?, ?>, RuleN<?>> {

    @SuppressWarnings("unchecked")
    public RuleRegistry(RegionContext context) {
        super(context, (Class<Rule<?>>) (Class<?>) Rule.class, DeclareRule.class);
    }

    @Override
    protected void handle(List<Region> regions, Event event, RuleN<?> listener, RuleTrigger<?, ?> trigger, EventPriority bukkitPriority) {

    }

    @Override
    public <E extends Event> RuleTriggerBuilder.BooleanRuleTriggerBuilder<E> registerOn(Class<E> eventClass) {
        return new InnerBooleanRuleTriggerBuilder<>(eventClass, this);
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


    protected static class InnerBooleanRuleTriggerBuilder<E extends Event> extends RuleTriggerBuilder.BooleanRuleTriggerBuilder<E> {

        private final RuleRegistry registry_;

        protected InnerBooleanRuleTriggerBuilder(Class<E> eventClass, RuleRegistry registry) {
            super(eventClass);
            this.registry_ = registry;
        }

        @Override public <T> RuleTriggerBuilder<E, T> having(ValueType<T> type) {
            return new InnerRuleTriggerBuilder<>(this, type, registry_);
        }
    }
    protected static class InnerRuleTriggerBuilder<E extends Event, T> extends RuleTriggerBuilder<E, T> {

        private final RuleRegistry registry_;

        protected InnerRuleTriggerBuilder(RuleTriggerBuilder<E, ?> other, ValueType<T> type, RuleRegistry registry) {
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
