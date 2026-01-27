package com.kntrel.mc.regionLib.test.mock;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public final class MockServer {

    private MockServer() {}



    public static Server mockServer() {
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);
        World mockWorld = MockWorld.mockWorld();
        when(server.getWorld(anyString())).thenReturn(mockWorld);
        when(server.getWorld(any(UUID.class))).thenReturn(mockWorld);
        when(server.getWorlds()).thenReturn(List.of(mockWorld));

        return server;
    }


}
