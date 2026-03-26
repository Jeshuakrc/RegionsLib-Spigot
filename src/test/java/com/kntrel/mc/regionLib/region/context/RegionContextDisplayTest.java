package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.display.RegionDisplayer;
import com.kntrel.mc.regionLib.region.display.DisplayToken;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockServer;
import com.kntrel.mc.regionLib.test.util.MemoryRegionRepository;
import com.kntrel.mc.regionLib.test.util.TestRegionContext;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegionContextDisplayTest {

    @Test
    void usesConfiguredDefaultRegionDisplayer() {
        Server server = MockServer.mockServer();
        Plugin plugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getName()).thenReturn("TestPlugin");

        RegionDisplayer displayer = mock(RegionDisplayer.class);

        AtomicReference<RegionContext> configuredContext = new AtomicReference<>();
        RegionContextConfig config = RegionContextConfig.build()
                .withRegionDisplayerFactory(ctx -> {
                    configuredContext.set(ctx);
                    return displayer;
                })
                .end();

        MockHierarchyRepository hierarchies = MockHierarchyRepository.ofSingle("sample");
        TestRegionContext context = new TestRegionContext(plugin, config, new MemoryRegionRepository(), hierarchies);

        Hierarchy hierarchy = hierarchies.getAll().getFirst();
        World world = server.getWorlds().getFirst();
        Region region = new Region(context, new BoundingBox(0, 0, 0, 10, 10, 10), world, "spawn", hierarchy);
        context.save(region);
        Player player = mock(Player.class);
        when(displayer.display(same(region), same(player))).thenReturn(new DisplayToken(displayer, 1L));

        region.display(player);

        assertSame(context, configuredContext.get());
        verify(displayer).display(same(region), same(player));
    }
}
