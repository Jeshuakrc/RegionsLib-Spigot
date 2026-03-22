package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.event.PlayerEnterRegionEvent;
import com.kntrel.mc.regionLib.event.PlayerLeaveRegionEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockServer;
import com.kntrel.mc.regionLib.test.util.MemoryRegionRepository;
import com.kntrel.mc.regionLib.test.util.TestRegionContext;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlayerSamplerTest {

    @Test
    void samplesDirtyPlayersAndTracksPlayersWithin() {
        Server server = MockServer.mockServer();
        PluginManager pluginManager = server.getPluginManager();
        Plugin plugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getName()).thenReturn("TestPlugin");

        World world = server.getWorlds().getFirst();
        Player player = mock(Player.class);
        AtomicReference<Location> location = new AtomicReference<>(new Location(world, 5, 64, 5));
        when(player.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(player.getLocation()).thenAnswer(invocation -> location.get());
        when(server.getOnlinePlayers()).thenReturn((Collection) List.of(player));

        MemoryRegionRepository repository = new MemoryRegionRepository();
        MockHierarchyRepository hierarchies = MockHierarchyRepository.ofSingle("sample");
        RegionContextConfig config = RegionContextConfig.build()
                .withPlayerSamplingPeriodTicks(20)
                .end();
        TestRegionContext context = new TestRegionContext(plugin, config, repository, hierarchies);

        Hierarchy hierarchy = hierarchies.getAll().getFirst();
        Region region = new Region(context, new BoundingBox(0, 0, 0, 10, 255, 10), world, "spawn", hierarchy);
        context.save((org.bukkit.entity.Entity) null, region);
        clearInvocations(pluginManager);

        PlayerSampler sampler = ((RegionContext) context).getPlayerSampler();
        sampler.onPlayerJoin(new PlayerJoinEvent(player, "joined"));
        sampler.sample();

        verify(pluginManager).callEvent(any(PlayerEnterRegionEvent.class));
        assertEquals(List.of(player), context.getPlayersWithin(region));
        assertEquals(List.of(player), context.getPlayersWithin(region.getId()));
        assertEquals(List.of(player), region.getPlayersWithin());
        assertEquals(List.of(player), region.getPlayerWithin());

        clearInvocations(pluginManager);
        location.set(new Location(world, 20, 64, 20));
        sampler.onPlayerMove(new PlayerMoveEvent(player, new Location(world, 5, 64, 5), location.get()));

        sampler.sample();

        verify(pluginManager).callEvent(any(PlayerLeaveRegionEvent.class));
        assertEquals(List.of(), context.getPlayersWithin(region));
        assertEquals(List.of(), context.getPlayersWithin(region.getId()));
        assertEquals(List.of(), region.getPlayersWithin());
    }

    @Test
    void movementToleranceDelaysSamplingUntilThresholdIsReached() {
        Server server = MockServer.mockServer();
        PluginManager pluginManager = server.getPluginManager();
        Plugin plugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getName()).thenReturn("TestPlugin");

        World world = server.getWorlds().getFirst();
        Player player = mock(Player.class);
        AtomicReference<Location> location = new AtomicReference<>(new Location(world, 5, 64, 5));
        when(player.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        when(player.getLocation()).thenAnswer(invocation -> location.get());
        when(server.getOnlinePlayers()).thenReturn((Collection) List.of(player));

        MemoryRegionRepository repository = new MemoryRegionRepository();
        MockHierarchyRepository hierarchies = MockHierarchyRepository.ofSingle("sample");
        RegionContextConfig config = RegionContextConfig.build()
                .withPlayerSamplingPeriodTicks(20)
                .withPlayerMovementTolerance(3D)
                .end();
        TestRegionContext context = new TestRegionContext(plugin, config, repository, hierarchies);

        Hierarchy hierarchy = hierarchies.getAll().getFirst();
        Region region = new Region(context, new BoundingBox(0, 0, 0, 10, 255, 10), world, "spawn", hierarchy);
        context.save((org.bukkit.entity.Entity) null, region);
        clearInvocations(pluginManager);

        PlayerSampler sampler = ((RegionContext) context).getPlayerSampler();
        sampler.onPlayerJoin(new PlayerJoinEvent(player, "joined"));
        sampler.sample();
        clearInvocations(pluginManager);

        Location smallMove = new Location(world, 6, 64, 5);
        location.set(smallMove);
        sampler.onPlayerMove(new PlayerMoveEvent(player, new Location(world, 5, 64, 5), smallMove));
        sampler.sample();

        verify(pluginManager, never()).callEvent(any(PlayerLeaveRegionEvent.class));
        assertEquals(List.of(player), context.getPlayersWithin(region));

        Location farMove = new Location(world, 20, 64, 20);
        location.set(farMove);
        sampler.onPlayerMove(new PlayerMoveEvent(player, smallMove, farMove));
        sampler.sample();

        verify(pluginManager).callEvent(any(PlayerLeaveRegionEvent.class));
        assertEquals(List.of(), context.getPlayersWithin(region));
    }

    @Test
    void invalidSamplingSettingsAreRejected() {
        Plugin plugin = mock(Plugin.class);
        Server server = MockServer.mockServer();
        when(plugin.getServer()).thenReturn(server);

        TestRegionContext context = new TestRegionContext(
                plugin,
                RegionContextConfig.defaultConfig(),
                new MemoryRegionRepository(),
                MockHierarchyRepository.ofSingle("sample")
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerSampler(context, 0, 0D)
        );
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerSampler(context, 1, -1D)
        );
    }
}
