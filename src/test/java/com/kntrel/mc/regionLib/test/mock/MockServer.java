package com.kntrel.mc.regionLib.test.mock;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public final class MockServer {

    private MockServer() {}



    public static Server mockServer() {
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getOnlinePlayers()).thenReturn((Collection) List.of());
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(task);
        when(scheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(task);
        World mockWorld = MockWorld.mockWorld();
        when(server.getWorld(anyString())).thenReturn(mockWorld);
        when(server.getWorld(any(UUID.class))).thenReturn(mockWorld);
        when(server.getWorlds()).thenReturn(List.of(mockWorld));

        return server;
    }


}
