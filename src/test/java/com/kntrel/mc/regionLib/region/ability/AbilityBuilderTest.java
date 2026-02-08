package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.testsupport.TestEvents;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityBuilderTest {

    @Test
    void infersPlayerFromPlayerEvent() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getLocation()).thenReturn(new Location(world, 1, 2, 3));

        Ability ability = Ability.on(TestEvents.SimplePlayerEvent.class)
                .when(event -> false)
                .done();

        AbilityTrigger<?> trigger = ability.triggers().iterator().next();
        @SuppressWarnings("unchecked")
        AbilityTrigger<TestEvents.SimplePlayerEvent> typedTrigger = (AbilityTrigger<TestEvents.SimplePlayerEvent>) trigger;
        Player attributed = typedTrigger.attribute(new TestEvents.SimplePlayerEvent(player));

        assertSame(player, attributed);
    }

    @Test
    void infersPlayerFromGetPlayerMethod() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 5, 6, 7);

        Ability ability = Ability.on(TestEvents.PlayerCarrierEvent.class)
                .at(event -> location)
                .when(event -> false)
                .done();

        AbilityTrigger<?> trigger = ability.triggers().iterator().next();
        @SuppressWarnings("unchecked")
        AbilityTrigger<TestEvents.PlayerCarrierEvent> typedTrigger = (AbilityTrigger<TestEvents.PlayerCarrierEvent>) trigger;
        Player attributed = typedTrigger.attribute(new TestEvents.PlayerCarrierEvent(player));

        assertSame(player, attributed);
    }

    @Test
    void failsWhenPlayerCannotBeInferred() {
        assertThrows(IllegalStateException.class, () -> Ability.on(TestEvents.NoPlayerEvent.class)
                .at(event -> null)
                .done());
    }

    @Test
    void buildsAbilityWithEventSpecificActionsAndAllowedDeniedHandlers() {
        AtomicInteger primaryAllowedCount = new AtomicInteger();
        AtomicInteger secondaryAllowedCount = new AtomicInteger();
        AtomicInteger primaryDeniedCount = new AtomicInteger();
        AtomicInteger secondaryDeniedCount = new AtomicInteger();

        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 1, 2, 3);

        Ability ability = Ability.on(TestEvents.SimplePlayerEvent.class)
                .at(e -> location)
                .ifAllowed((e, r) -> primaryAllowedCount.incrementAndGet())
                .ifDenied((e, r) -> primaryDeniedCount.incrementAndGet())
                .alsoOn(TestEvents.PlayerCarrierEvent.class)
                .at(e -> location)
                .ifAllowed((e, r) -> secondaryAllowedCount.incrementAndGet())
                .ifDenied((e, r) -> secondaryDeniedCount.incrementAndGet())
                .named("testAbility");

        List<Region> regions = List.of();

        ability.onAllowed(new TestEvents.SimplePlayerEvent(player), regions);
        ability.onAllowed(new TestEvents.PlayerCarrierEvent(player), regions);
        ability.onDenied(new TestEvents.SimplePlayerEvent(player), regions);
        ability.onDenied(new TestEvents.PlayerCarrierEvent(player), regions);

        assertEquals(1, primaryAllowedCount.get());
        assertEquals(1, secondaryAllowedCount.get());
        assertEquals(1, primaryDeniedCount.get());
        assertEquals(1, secondaryDeniedCount.get());
    }
}
