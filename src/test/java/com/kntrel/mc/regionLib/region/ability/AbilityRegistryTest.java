package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.test.Regions;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockPlugin;
import com.kntrel.mc.regionLib.test.util.MemoryRegionRepository;
import com.kntrel.mc.regionLib.test.util.TestRegionContext;
import com.kntrel.mc.regionLib.test.util.TestEvents;
import com.kntrel.util.Priority;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityRegistryTest {

    private Player player;
    private World world;
    private Location location;
    private RegionContext context;
    private Hierarchy hierarchy;
    private RegionRepository repository;
    private List<Region> regions;

    private static class ExposedAbilityRegistry extends AbilityRegistry {
        ExposedAbilityRegistry(RegionContext context, Permission.OverlapMode mode) {
            super(context, mode);
        }

        void handleEvent(Event event, RegionTrigger<?> trigger) {
            super.handle(event, new EventKey(trigger.eventClass(), trigger.bukkitPriority()));
        }
    }

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        context = new TestRegionContext(
                new MockPlugin(),
                RegionContextConfig.defaultConfig(),
                new MemoryRegionRepository(),
                MockHierarchyRepository.ofSingle("hierarchy", 3)
        );
        repository = context.getRegionRepository();
        hierarchy = context.getHierarchyRepository().getAll().getFirst();

        world = context.getServer().getWorlds().getFirst();
        location = new Location(world, 0, 64, 0);

        regions = Regions.newRegions(10, context, hierarchy, "region_");
        regions.forEach(r -> {
            r.resize(10, 100, 10, -10, 0, -10);
            r.save();
        });

    }

    @Test
    void overlapModeAnyAllowsWhenAnyRegionAllows() {
        Region region = regions.get(1);
        hierarchy.addGroup("anyGroup", hierarchy.getHighestLevel() + 1, List.of("any"));
        region.addPermission(player, hierarchy.getGroup("anyGroup").orElseThrow());
        region.save();

        testOverlapMode(Permission.OverlapMode.ANY, "any", true);
    }

    @Test
    void overlapModeAllDeniesWhenAnyRegionDenies() {
        Region region = regions.get(1);
        hierarchy.addGroup("anyGroup", hierarchy.getHighestLevel() + 1, List.of("all"));
        region.addPermission(player, hierarchy.getGroup("anyGroup").orElseThrow());
        region.save();

        testOverlapMode(Permission.OverlapMode.ALL,  "all", false);

        regions.stream().filter(r -> !r.equals(region)).forEach(r -> {
            r.addPermission(player, hierarchy.getGroup("anyGroup").orElseThrow());
            r.save();
        });

        testOverlapMode(Permission.OverlapMode.ALL,  "all", true);
    }

    @Test
    void overlapModeNewestUsesLastRegion() {
        Region regionA = regions.getLast();
        Region regionB = regions.get(7);
        hierarchy.addGroup("anyGroup", hierarchy.getHighestLevel() + 1, List.of("newest"));
        regionA.addPermission(player, hierarchy.getGroup("anyGroup").orElseThrow());
        regionB.addPermission(player, hierarchy.getGroup("anyGroup").orElseThrow());
        regionA.save();
        regionB.save();

        testOverlapMode(Permission.OverlapMode.NEWEST,  "newest", true);

        regionA.destroy();
        regionA.save();
        testOverlapMode(Permission.OverlapMode.NEWEST, "newest", false);

        regions.get(8).destroy();
        regions.get(8).save();
        testOverlapMode(Permission.OverlapMode.NEWEST,  "newest", true);
    }

    @Test
    void overlapModeOldestUsesFirstRegion() {
        Region regionA = regions.getFirst();
        Region regionB = regions.get(2);
        hierarchy.addGroup("anyGroup", hierarchy.getHighestLevel() + 1, List.of("oldest"));
        regionA.addPermission(player, hierarchy.getGroup("anyGroup").orElseThrow());
        regionB.addPermission(player, hierarchy.getGroup("anyGroup").orElseThrow());
        regionA.save();
        regionB.save();

        testOverlapMode(Permission.OverlapMode.OLDEST, "oldest", true);

        regionA.destroy();
        regionA.save();
        testOverlapMode(Permission.OverlapMode.OLDEST, "oldest", false);

        regions.get(1).destroy();
        regions.get(1).save();
        testOverlapMode(Permission.OverlapMode.OLDEST, "oldest", true);
    }

    @Test
    void superAbilityMustPassBeforeSubAbilityHandles() {
        AtomicBoolean superPasses = new AtomicBoolean(false);

        Ability superAbility = Ability.on(TestEvents.LocationEvent.class)
                .by(e -> player)
                .at(e -> location)
                .when(e -> superPasses.get())
                .named("super");

        AtomicInteger allowedCount = new AtomicInteger();
        Ability subAbility = Ability.on(TestEvents.LocationEvent.class)
                .by(e -> player)
                .at(e -> location)
                .prioritize(1)
                .extend(superAbility)
                .ifAllowed((e, r) -> allowedCount.incrementAndGet())
                .named("sub");

        hierarchy.addGroup("testGroup", hierarchy.getHighestLevel() + 1, List.of("sub"));
        Region region = regions.getFirst();
        region.addPermission(player, hierarchy.getGroup("testGroup").orElseThrow());
        region.save();

        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(context, Permission.OverlapMode.ANY);
        registry.register(superAbility);
        registry.register(subAbility);

        RegionTrigger<?> trigger = subAbility.triggers().iterator().next();
        registry.handleEvent(new TestEvents.LocationEvent(location), trigger);

        assertEquals(0, allowedCount.get());

        superPasses.set(true);
        registry.handleEvent(new TestEvents.LocationEvent(location), trigger);
        assertEquals(1, allowedCount.get());
    }

    @Test
    void registerRejectsSuperAbilityWithNoSharedTriggers() {
        Ability superAbility = Ability.on(TestEvents.SecondaryLocationEvent.class)
                .by(e -> player)
                .at(e -> location)
                .when(e -> true)
                .named("super");

        Ability subAbility = Ability.on(TestEvents.LocationEvent.class)
                .by(e -> player)
                .at(e -> location)
                .when(e -> true)
                .extend(superAbility)
                .named("sub");

        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(context, Permission.OverlapMode.ANY);

        registry.register(superAbility);
        assertThrows(IllegalStateException.class, () -> registry.register(subAbility));
    }

    @Test
    void noTriggerIsTestedMoreThanOnce() {
        AtomicInteger testACount = new AtomicInteger(), testBCount = new AtomicInteger();

        Ability abilityA = Ability.on(TestEvents.SimplePlayerEvent.class)
                .at(e -> location)
                .when(e -> {
                    testACount.incrementAndGet();
                    return true;
                })
                .named("abilityA");

        Ability abilityB = Ability.on(TestEvents.SimplePlayerEvent.class)
                .at(e -> location)
                .when(e -> {
                    testBCount.incrementAndGet();
                    return true;
                })
                .extend(abilityA)
                .prioritize(Priority.HIGH)
                .named("abilityB");

        Region region = regions.getFirst();
        hierarchy.addGroup("testGroup", hierarchy.getHighestLevel() + 1, List.of("abilityA", "abilityB"));
        region.addPermission(player, hierarchy.getGroup("testGroup").orElseThrow());
        region.save();

        repository.save(region);

        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(context, Permission.OverlapMode.ANY);
        registry.register(abilityA);
        registry.register(abilityB);

        RegionTrigger<?> trigger = abilityA.triggers().iterator().next();
        registry.handleEvent(new TestEvents.SimplePlayerEvent(player), trigger);

        assertEquals(1, testACount.get(), "Ability A's trigger was tested more than once");
        assertEquals(1, testBCount.get(), "Ability B's trigger was tested more than once");

        testACount.set(0); 
        testBCount.set(0);
        AtomicInteger testCCount = new AtomicInteger();
        Ability abilityC = Ability.on(TestEvents.SimplePlayerEvent.class)
                .at(e -> location)
                .when(e -> {
                    testCCount.incrementAndGet();
                    return false;
                })
                .extend(abilityA)
                .prioritize(Priority.HIGHEST)
                .named("abilityC");

        registry.register(abilityC);

        registry.handleEvent(new TestEvents.SimplePlayerEvent(player), trigger);
        assertEquals(1, testACount.get(), "Ability A's trigger was tested more than once");
        assertEquals(1, testBCount.get(), "Ability B's trigger was tested more than once");
        assertEquals(1, testCCount.get(), "Ability C's trigger was tested more than once");
    }

    @Test
    void highestPriorityRunsFirst() {

    }


    //HELPERS
    private Ability createAbility(Class<? extends Event> eventClass, String name) {
        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger deniedCount = new AtomicInteger();
        return Ability.on(eventClass)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .ifAllowed((event, regions) -> allowedCount.incrementAndGet())
                .ifDenied((event, regions) -> deniedCount.incrementAndGet())
                .named(name);
    }

    private Ability createAbilityWithCounters(Class<? extends Event> eventClass, String name, AtomicInteger allowedCount, AtomicInteger deniedCount) {
        return Ability.on(eventClass)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .ifAllowed((event, regions) -> allowedCount.incrementAndGet())
                .ifDenied((event, regions) -> deniedCount.incrementAndGet())
                .named(name);
    }

    private void testOverlapMode(Permission.OverlapMode mode, String abilityName, boolean expectedAllowed) {
        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger deniedCount = new AtomicInteger();
        Ability ability = createAbilityWithCounters(TestEvents.LocationEvent.class, abilityName, allowedCount, deniedCount);

        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(context, mode);
        registry.register(ability);

        RegionTrigger<?> trigger = ability.triggers().iterator().next();
        registry.handleEvent(new TestEvents.LocationEvent(location), trigger);

        if (expectedAllowed) {
            assertEquals(1, allowedCount.get());
            assertEquals(0, deniedCount.get());
        } else {
            assertEquals(0, allowedCount.get());
            assertEquals(1, deniedCount.get());
        }
    }

}
