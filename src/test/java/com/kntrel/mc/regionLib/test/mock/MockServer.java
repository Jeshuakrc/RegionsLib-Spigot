package com.kntrel.mc.regionLib.test.mock;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;

import static org.mockito.Mockito.*;

public final class MockServer {

    private MockServer() {}

    private static Server MOCK_SERVER = null;


    public static Server mockServer() {
        if (MOCK_SERVER != null) {
            return MOCK_SERVER;
        }

        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);
        World mockWorld = MockWorld.mockWorld();
        when(server.getWorld(anyString())).thenReturn(mockWorld);

        MOCK_SERVER = server;
        return MOCK_SERVER;
    }


}
