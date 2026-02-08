package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.listen.ReflectiveListernerRegistry;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import org.bukkit.event.Event;
import java.util.List;

public class RuleRegistry extends ReflectiveListernerRegistry<RegionTrigger<?>, Rule<?>> {

    @SuppressWarnings("unchecked")
    public RuleRegistry(RegionContext context) {
        super(context, (Class<Rule<?>>) (Class<?>) Rule.class, DeclareRule.class);
    }

    @Override @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void handle(List<Region> regions, Event event, Rule<?> rule, RegionTrigger<?> trigger) {
        for (Region region : regions) {
            Object value = region.getRuleValue(rule).orElse(null);
            if (value != null) { ((Rule) rule).fire(value, event, region); }
            else { rule.fireOnAbsent(event, region); }
        }
    }
}
