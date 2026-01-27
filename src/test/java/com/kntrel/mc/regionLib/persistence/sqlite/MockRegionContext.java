package com.kntrel.mc.regionLib.persistence.sqlite;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockServer;
import com.kntrel.util.cache.ConcurrentRLUCache;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class MockRegionContext {

    public static RegionContext mockContext() {
        ExecutorService executorService = new AbstractExecutorService() {
            private volatile boolean shutdown;
            @Override public void shutdown() { shutdown = true; }
            @Override public @NotNull List<Runnable> shutdownNow() { shutdown = true; return List.of(); }
            @Override public boolean isShutdown() { return shutdown; }
            @Override public boolean isTerminated() { return shutdown; }
            @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
            @Override public void execute(Runnable command) { command.run(); }
        };

        DataBase dataBase = spy(DataBaseTest.memoryDatabase());
        Server server = MockServer.mockServer();
        HierarchyRepository hierarchyRepository = MockHierarchyRepository.ofSingle("hierarchy", 3);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(server);
        return new RegionContext(
                RegionContextConfig.build().withCacheCapacity(10).end(),
                plugin,
                ctx -> {
                    QueryParser queryParser = new QueryParser(ctx);
                    return new SQLiteRegionRepository(ctx, dataBase, queryParser, executorService);
                },
                ctx -> hierarchyRepository
        );
    }

}
