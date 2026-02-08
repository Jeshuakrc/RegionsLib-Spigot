package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.listen.RegionTrigger;
import com.kntrel.mc.regionLib.testsupport.TestContextFactory;
import com.kntrel.mc.regionLib.testsupport.TestEvents;
import com.kntrel.mc.regionLib.testsupport.TestRegionReadRepository;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbilityRegistryTest {

    private static class ExposedAbilityRegistry extends AbilityRegistry {
        ExposedAbilityRegistry(RegionContext context, Permission.OverlapMode mode) {
            super(context, mode);
        }

        void handleEvent(Event event, RegionTrigger<?> trigger) {
            super.handle(event, new EventKey(trigger.eventClass(), trigger.bukkitPriority()));
        }
    }

    @Test
    void overlapModeAnyAllowsWhenAnyRegionAllows() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);

        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger deniedCount = new AtomicInteger();
        Ability ability = Ability.on(TestEvents.LocationEvent.class)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .ifAllowed((event, regions) -> allowedCount.incrementAndGet())
                .ifDenied((event, regions) -> deniedCount.incrementAndGet())
                .named("any");

        Region regionA = mock(Region.class);
        Region regionB = mock(Region.class);
        when(regionA.checkAbility(player, ability)).thenReturn(false);
        when(regionB.checkAbility(player, ability)).thenReturn(true);

        TestRegionReadRepository repository = new TestRegionReadRepository();
        repository.setRegions(List.of(regionA, regionB));
        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(TestContextFactory.mockContext(repository), Permission.OverlapMode.ANY);
        registry.register(ability);

        RegionTrigger<?> trigger = ability.triggers().iterator().next();
        registry.handleEvent(new TestEvents.LocationEvent(location), trigger);

        assertEquals(1, allowedCount.get());
        assertEquals(0, deniedCount.get());
    }

    @Test
    void overlapModeAllDeniesWhenAnyRegionDenies() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);

        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger deniedCount = new AtomicInteger();
        Ability ability = Ability.on(TestEvents.LocationEvent.class)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .ifAllowed((event, regions) -> allowedCount.incrementAndGet())
                .ifDenied((event, regions) -> deniedCount.incrementAndGet())
                .named("all");

        Region regionA = mock(Region.class);
        Region regionB = mock(Region.class);
        when(regionA.checkAbility(player, ability)).thenReturn(true);
        when(regionB.checkAbility(player, ability)).thenReturn(false);

        TestRegionReadRepository repository = new TestRegionReadRepository();
        repository.setRegions(List.of(regionA, regionB));
        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(TestContextFactory.mockContext(repository), Permission.OverlapMode.ALL);
        registry.register(ability);

        RegionTrigger<?> trigger = ability.triggers().iterator().next();
        registry.handleEvent(new TestEvents.LocationEvent(location), trigger);

        assertEquals(0, allowedCount.get());
        assertEquals(1, deniedCount.get());
    }

    @Test
    void overlapModeNewestUsesLastRegion() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);

        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger deniedCount = new AtomicInteger();
        Ability ability = Ability.on(TestEvents.LocationEvent.class)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .ifAllowed((event, regions) -> allowedCount.incrementAndGet())
                .ifDenied((event, regions) -> deniedCount.incrementAndGet())
                .named("newest");

        Region oldest = mock(Region.class);
        Region newest = mock(Region.class);
        when(oldest.checkAbility(player, ability)).thenReturn(false);
        when(newest.checkAbility(player, ability)).thenReturn(true);

        TestRegionReadRepository repository = new TestRegionReadRepository();
        repository.setRegions(List.of(oldest, newest));
        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(TestContextFactory.mockContext(repository), Permission.OverlapMode.NEWEST);
        registry.register(ability);

        RegionTrigger<?> trigger = ability.triggers().iterator().next();
        registry.handleEvent(new TestEvents.LocationEvent(location), trigger);

        assertEquals(1, allowedCount.get());
        assertEquals(0, deniedCount.get());
    }

    @Test
    void overlapModeOldestUsesFirstRegion() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);

        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger deniedCount = new AtomicInteger();
        Ability ability = Ability.on(TestEvents.LocationEvent.class)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .ifAllowed((event, regions) -> allowedCount.incrementAndGet())
                .ifDenied((event, regions) -> deniedCount.incrementAndGet())
                .named("oldest");

        Region oldest = mock(Region.class);
        Region newest = mock(Region.class);
        when(oldest.checkAbility(player, ability)).thenReturn(false);
        when(newest.checkAbility(player, ability)).thenReturn(true);

        TestRegionReadRepository repository = new TestRegionReadRepository();
        repository.setRegions(List.of(oldest, newest));
        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(TestContextFactory.mockContext(repository), Permission.OverlapMode.OLDEST);
        registry.register(ability);

        RegionTrigger<?> trigger = ability.triggers().iterator().next();
        registry.handleEvent(new TestEvents.LocationEvent(location), trigger);

        assertEquals(0, allowedCount.get());
        assertEquals(1, deniedCount.get());
    }

    @Test
    void superAbilityMustPassBeforeSubAbilityHandles() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);

        Ability superAbility = Ability.on(TestEvents.LocationEvent.class)
                .by(event -> player)
                .at(event -> location)
                .when(event -> false)
                .named("super");

        AtomicInteger allowedCount = new AtomicInteger();
        Ability subAbility = Ability.on(TestEvents.LocationEvent.class)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .prioritize(1)
                .extend(superAbility)
                .ifAllowed((event, regions) -> allowedCount.incrementAndGet())
                .named("sub");

        Region region = mock(Region.class);
        when(region.checkAbility(player, subAbility)).thenReturn(true);

        TestRegionReadRepository repository = new TestRegionReadRepository();
        repository.setRegions(List.of(region));
        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(TestContextFactory.mockContext(repository), Permission.OverlapMode.ANY);
        registry.register(superAbility);
        registry.register(subAbility);

        RegionTrigger<?> trigger = subAbility.triggers().iterator().next();
        registry.handleEvent(new TestEvents.LocationEvent(location), trigger);

        assertEquals(0, allowedCount.get());
    }

    @Test
    void registerRejectsSuperAbilityWithNoSharedTriggers() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);

        Ability superAbility = Ability.on(TestEvents.SecondaryLocationEvent.class)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .named("super");

        Ability subAbility = Ability.on(TestEvents.LocationEvent.class)
                .by(event -> player)
                .at(event -> location)
                .when(event -> true)
                .extend(superAbility)
                .named("sub");

        TestRegionReadRepository repository = new TestRegionReadRepository();
        ExposedAbilityRegistry registry = new ExposedAbilityRegistry(TestContextFactory.mockContext(repository), Permission.OverlapMode.ANY);

        registry.register(superAbility);
        assertThrows(IllegalStateException.class, () -> registry.register(subAbility));
    }
}
