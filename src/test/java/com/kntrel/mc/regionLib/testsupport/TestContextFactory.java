package com.kntrel.mc.regionLib.testsupport;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestContextFactory {

    private TestContextFactory() {}

    public static RegionContext mockContext(RegionReadRepository repository) {
        RegionContext context = mock(RegionContext.class);
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);

        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(context.getPlugin()).thenReturn(plugin);
        when(context.getServer()).thenReturn(server);
        when(context.getHotRegionRepository()).thenReturn(repository);

        return context;
    }
}
