package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import com.kntrel.mc.regionLib.testsupport.TestContextFactory;
import com.kntrel.mc.regionLib.testsupport.TestEvents;
import com.kntrel.mc.regionLib.testsupport.TestRegionReadRepository;
import com.kntrel.mc.regionLib.util.valueType.ValueHolder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleRegistryTest {

    private static class ExposedRuleRegistry extends RuleRegistry {
        ExposedRuleRegistry(com.kntrel.mc.regionLib.region.context.RegionContext context) {
            super(context);
        }

        void handleEvent(Event event, RegionTrigger<?> trigger) {
            super.handle(event, new EventKey(trigger.eventClass(), trigger.bukkitPriority()));
        }
    }

    @Test
    void handlesRuleValuesAndAbsentValuesDirectly() {
        AtomicInteger actionCount = new AtomicInteger();
        AtomicInteger absentCount = new AtomicInteger();

        Rule<Boolean> rule = Rule.on(TestEvents.LocationEvent.class)
                .at(TestEvents.LocationEvent::getLocation)
                .ifTrue()
                .then(e -> actionCount.incrementAndGet())
                .ifAbsent(e -> absentCount.incrementAndGet())
                .named("valueRule");

        Region regionWithValue = mock(Region.class);
        Region regionWithoutValue = mock(Region.class);
        when(regionWithValue.getRuleValue("valueRule")).thenReturn(Optional.of(ValueHolder.of(true)));
        when(regionWithoutValue.getRuleValue("valueRule")).thenReturn(Optional.empty());

        TestRegionReadRepository repository = new TestRegionReadRepository();
        repository.setRegions(List.of(regionWithValue, regionWithoutValue));

        ExposedRuleRegistry registry = new ExposedRuleRegistry(TestContextFactory.mockContext(repository));
        registry.register(rule);

        World world = mock(World.class);
        TestEvents.LocationEvent event = new TestEvents.LocationEvent(new Location(world, 0, 64, 0));
        RegionTrigger<?> trigger = rule.triggers().iterator().next();

        registry.handleEvent(event, trigger);

        assertEquals(1, actionCount.get());
        assertEquals(1, absentCount.get());
    }
}
