package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.testsupport.TestEvents;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

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
}
