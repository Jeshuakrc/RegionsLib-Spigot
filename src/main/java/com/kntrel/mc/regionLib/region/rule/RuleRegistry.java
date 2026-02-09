package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.event.RuleTriggeredEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.listen.Place;
import com.kntrel.mc.regionLib.region.listen.ReflectiveListernerRegistry;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import com.kntrel.mc.regionLib.util.valueType.ValueHolder;
import org.bukkit.event.Event;
import java.util.List;

/**
 * Registry that manages rule definitions and trigger handling.
 */
public class RuleRegistry extends ReflectiveListernerRegistry<RegionTrigger<?>, Rule<?>> {

    @SuppressWarnings("unchecked")
    public RuleRegistry(RegionContext context) {
        super(context, (Class<Rule<?>>) (Class<?>) Rule.class, DeclareRule.class);
    }

    @Override @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void handle(List<Region> regions, Event event, Rule<?> rule, RegionTrigger<?> trigger, Place place) {
        for (Region region : regions) {
            ValueHolder<?> valueHolder = region.getRuleValue(rule.name()).orElse(null);

            RuleTriggeredEvent ev = new RuleTriggeredEvent(region, rule, valueHolder, trigger, event, place);
            this.plugin_.getServer().getPluginManager().callEvent(ev);
            if (ev.isCancelled()) { continue; }

            if (valueHolder != null) { ((Rule) rule).fire(valueHolder.get(), event, region); }
            else { rule.fireOnAbsent(event, region); }
        }
    }

    @Override
    protected void registerTrigger(Rule<?> listener, RegionTrigger<?> trigger) {
        if (trigger.eventClass().equals(RuleTriggeredEvent.class)) {
            throw new IllegalArgumentException("A rule cannot listen to RuleTriggeredEvent.");
        }
        super.registerTrigger(listener, trigger);
    }
}
