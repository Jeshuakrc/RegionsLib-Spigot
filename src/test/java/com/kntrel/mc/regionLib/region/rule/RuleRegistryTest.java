package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.test.Regions;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockPlugin;
import com.kntrel.mc.regionLib.test.util.MemoryRegionRepository;
import com.kntrel.mc.regionLib.test.util.TestEvents;
import com.kntrel.mc.regionLib.test.util.TestRegionContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RuleRegistryTest {

    private RegionContext context;
    private World world;
    private Location location;
    private RegionRepository repository;
    private List<Region> regions;

    private static class ExposedRuleRegistry extends RuleRegistry {
        ExposedRuleRegistry(com.kntrel.mc.regionLib.region.context.RegionContext context) {
            super(context);
        }

        void handleEvent(Event event, RegionTrigger<?> trigger) {
            super.handle(event, new EventKey(trigger.eventClass(), trigger.bukkitPriority()));
        }
    }

    @BeforeEach
    void setUp() {
        context = new TestRegionContext(
                new MockPlugin(),
                RegionContextConfig.defaultConfig(),
                new MemoryRegionRepository(),
                MockHierarchyRepository.ofSingle("hierarchy")
        );

        world = context.getServer().getWorlds().getFirst();
        location = new Location(world, 0, 64, 0);
        repository = context.getRegionRepository();

        regions = Regions.newRegions(10, context, context.getHierarchyRepository().getAll().getFirst(), "region_");
        regions.forEach(r -> {
            r.resize(10, 100, 10, -10, 0, -10);
            r.save();
        });
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

        Region regionWithValue = regions.getFirst();
        regionWithValue.setRuleValue("valueRule", true);
        regionWithValue.save();

        ExposedRuleRegistry registry = new ExposedRuleRegistry(context);
        registry.register(rule);

        TestEvents.LocationEvent event = new TestEvents.LocationEvent(location);
        RegionTrigger<?> trigger = rule.triggers().iterator().next();

        registry.handleEvent(event, trigger);

        assertEquals(1, actionCount.get());
        assertEquals(9, absentCount.get());
    }
}
