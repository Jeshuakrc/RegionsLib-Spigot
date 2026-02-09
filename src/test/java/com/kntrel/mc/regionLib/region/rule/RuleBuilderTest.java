package com.kntrel.mc.regionLib.region.rule;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.test.util.TestEvents;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleBuilderTest {

    private World world;
    private Location location;

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        location = new Location(world, 1, 2, 3);
    }

    @Test
    void buildsRuleWithEventSpecificActionsAndAbsentHandlers() {
        AtomicInteger primaryActionCount = new AtomicInteger();
        AtomicInteger secondaryActionCount = new AtomicInteger();
        AtomicInteger primaryAbsentCount = new AtomicInteger();
        AtomicInteger secondaryAbsentCount = new AtomicInteger();

        Rule<Boolean> rule = Rule.on(TestEvents.LocationEvent.class)
                .at(TestEvents.LocationEvent::getLocation)
                .when(event -> false)
                .then((value, event, region) -> primaryActionCount.incrementAndGet())
                .ifAbsent((event, region) -> primaryAbsentCount.incrementAndGet())
                .alsoOn(TestEvents.SecondaryLocationEvent.class)
                .at(TestEvents.SecondaryLocationEvent::getLocation)
                .when(event -> false)
                .then((value, event, region) -> secondaryActionCount.incrementAndGet())
                .ifAbsent((event, region) -> secondaryAbsentCount.incrementAndGet())
                .named("testRule");

        Region region = mock(Region.class);
        TestEvents.LocationEvent primaryEvent = new TestEvents.LocationEvent(location);
        TestEvents.SecondaryLocationEvent secondaryEvent = new TestEvents.SecondaryLocationEvent(new Location(world, 4, 5, 6));

        rule.fire(true, primaryEvent, region);
        rule.fire(true, secondaryEvent, region);
        rule.fireOnAbsent(primaryEvent, region);
        rule.fireOnAbsent(secondaryEvent, region);

        assertEquals(1, primaryActionCount.get());
        assertEquals(1, secondaryActionCount.get());
        assertEquals(1, primaryAbsentCount.get());
        assertEquals(1, secondaryAbsentCount.get());
    }

    @Test
    void thenCancelFailsOnNonCancellableEvent() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> Rule.on(TestEvents.LocationEvent.class)
                .at(TestEvents.LocationEvent::getLocation)
                .thenCancel()
                .named("cancelRule"));
        assertTrue(error.getMessage().contains("not cancellable"));
    }

    @Test
    void requiresActionBeforeBuild() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> Rule.on(TestEvents.LocationEvent.class)
                .at(TestEvents.LocationEvent::getLocation)
                .done());
        assertTrue(error.getMessage().contains("Rule trigger does nothing"));
    }

    @Test
    void thenCancelSetsCancelledOnCancellableEvent() {
        TestEvents.CancellableEvent event = new TestEvents.CancellableEvent();
        Rule<Boolean> rule = Rule.on(TestEvents.CancellableEvent.class)
                .at(e -> null)
                .thenCancel()
                .named("cancelCancellable");

        rule.fire(true, event, mock(Region.class));

        assertTrue(event.isCancelled());
    }
}
