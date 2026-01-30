package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.trigger.ReflectiveTriggerListernerRegistry;
import org.bukkit.event.Event;
import java.util.List;

public class RuleRegistry extends ReflectiveTriggerListernerRegistry<RuleTrigger<?, ?>, Rule<?>> {

    @SuppressWarnings("unchecked")
    public RuleRegistry(RegionContext context) {
        super(context, (Class<Rule<?>>) (Class<?>) Rule.class, DeclareRule.class);
    }

    @Override @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handle(List<Region> regions, Event event, Rule<?> rule, RuleTrigger<?, ?> trigger) {
        for (Region region : regions) {
            Object val = region.getRuleValue(rule.name());
            if (val == null) { continue; }

            RuleTrigger trg = trigger;
            if (!trg.test(val, region, event)) { continue; }

            trg.fire(val, region, event);
        }
    }
}
