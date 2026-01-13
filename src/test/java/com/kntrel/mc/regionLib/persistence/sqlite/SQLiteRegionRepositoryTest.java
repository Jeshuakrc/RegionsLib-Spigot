package com.kntrel.mc.regionLib.persistence.sqlite;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.test.Regions;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockServer;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class SQLiteRegionRepositoryTest {

    private static final Gson GSON = new Gson();

    private RegionContext regionContext;
    private Server server;
    private QueryParser queryParser;
    private DataBase dataBase;
    private ExecutorService executorService;
    private HierarchyRepository hierarchyRepository;
    private SQLiteRegionRepository regionRepository;


    @BeforeEach
    void setUp() {
        this.executorService = new AbstractExecutorService() {
            private volatile boolean shutdown;
            @Override public void shutdown() { shutdown = true; }
            @Override public @NotNull List<Runnable> shutdownNow() { shutdown = true; return List.of(); }
            @Override public boolean isShutdown() { return shutdown; }
            @Override public boolean isTerminated() { return shutdown; }
            @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
            @Override public void execute(Runnable command) { command.run(); }
        };

        this.dataBase = spy(DataBaseTest.memoryDatabase());
        this.server = MockServer.mockServer();
        this.hierarchyRepository = MockHierarchyRepository.ofSingle("hierarchy");
        Plugin plugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(this.server);
        this.regionContext = new RegionContext(plugin, ctx -> {
            this.queryParser = new QueryParser(ctx);
            return new SQLiteRegionRepository(this.server, ctx, this.dataBase, this.queryParser, this.executorService, this.hierarchyRepository);
        });
        this.regionRepository = (SQLiteRegionRepository) this.regionContext.getRegionRepository();
    }


    @Test
    void testInserts() {
        List<Region> regions = Regions.newRegions(10, this.regionContext, this.hierarchyRepository.getAll().getFirst(), "Region ");

        this.regionRepository.save(regions);

        ArgumentCaptor<List<Object>> inserts = ArgumentCaptor.forClass(List.class),
                                     updates = ArgumentCaptor.forClass(List.class),
                                     deletes = ArgumentCaptor.forClass(List.class);

        assertDoesNotThrow(() ->
            verify(this.dataBase).write(inserts.capture(), updates.capture(), deletes.capture())
        );

        List<Object> insertValues = inserts.getValue(),
                     updateValues = updates.getValue(),
                     deleteValues = deletes.getValue();

        assertTrue(updateValues.isEmpty());
        assertTrue(deleteValues.isEmpty());
        assertFalse(insertValues.isEmpty());
        assertEquals(10, insertValues.size());

        for (Region r : regions) {
            r.addPermission(UUID.randomUUID(), 1);
            r.addPermission(UUID.randomUUID(), 1);
            r.addPermission(UUID.randomUUID(), 1);
        }

        this.regionRepository.save(regions);

        assertDoesNotThrow(() ->
            verify(this.dataBase, times(2)).write(inserts.capture(), updates.capture(), deletes.capture())
        );

        insertValues = inserts.getValue();
        updateValues = updates.getValue();
        deleteValues = deletes.getValue();

        assertTrue(updateValues.isEmpty());
        assertTrue(deleteValues.isEmpty());
        assertFalse(insertValues.isEmpty());
        assertEquals(30, insertValues.size()); // 3 permissions per region x 10 regions

        for (Object o : insertValues) {
            DTO.Permission perm = assertInstanceOf(DTO.Permission.class, o);
            assertNotNull(perm);
            assertEquals(1, perm.level());
        }

        regions = Regions.newRegions(10, this.regionContext, this.hierarchyRepository.getAll().getFirst(), "Another Region ");
        for (Region r : regions) {
            r.addPermission(UUID.randomUUID(), 1);
            r.addPermission(UUID.randomUUID(), 1);

            RegionDataContainer dataContainer = r.getDataContainer();
            dataContainer.add(new RegionData("basic_data", "some_value"));
        }
        this.regionRepository.save(regions);

        assertDoesNotThrow(() ->
                verify(this.dataBase, times(3)).write(inserts.capture(), updates.capture(), deletes.capture())
        );

        insertValues = inserts.getValue();
        updateValues = updates.getValue();
        deleteValues = deletes.getValue();

        assertTrue(updateValues.isEmpty());
        assertTrue(deleteValues.isEmpty());
        assertFalse(insertValues.isEmpty());
        assertEquals(40, insertValues.size()); // 2 permissions + 1 data + 1 region per region x 10 regions
    }

    @Test
    void testUpdates() {
        List<Region> regions = Regions.newRegions(10, this.regionContext, this.hierarchyRepository.getAll().getFirst(), "Region ");
        for (Region r : regions) {
            RegionDataContainer dataContainer = r.getDataContainer();
            dataContainer.add(new RegionData("val1", "initial_value"));
            dataContainer.add(new RegionData("val2", "initial_value"));
            dataContainer.add(new RegionData("val3", "initial_value"));
        }

        this.regionRepository.save(regions);

        for (Region r : regions) {
            r.setName(r.getName() + " Updated");
            RegionDataContainer dataContainer = r.getDataContainer();
            dataContainer.get("val1").setValue("updated_value");
            dataContainer.get("val2").setValue("updated_value");
        }

        this.regionRepository.save(regions);

        ArgumentCaptor<List<Object>> inserts = ArgumentCaptor.forClass(List.class),
                                     updates = ArgumentCaptor.forClass(List.class),
                                     deletes = ArgumentCaptor.forClass(List.class);

        assertDoesNotThrow(() ->
            verify(this.dataBase, times(2)).write(inserts.capture(), updates.capture(), deletes.capture())
        );

        List<Object> insertValues = inserts.getValue(),
                     updateValues = updates.getValue(),
                     deleteValues = deletes.getValue();

        assertTrue(insertValues.isEmpty());
        assertTrue(deleteValues.isEmpty());
        assertFalse(updateValues.isEmpty());
        assertEquals(30, updateValues.size());

        for (Object o : updateValues) {
            if (o instanceof DTO.Region regionDto) {
                assertTrue(regionDto.name().endsWith(" Updated"));
            } else if (o instanceof DTO.Data dataDto) {
                JsonElement json = GSON.fromJson(dataDto.value(), JsonElement.class);
                assertEquals(new JsonPrimitive("updated_value"), json);
            } else {
                fail("Unexpected DTO type in updates: " + o.getClass().getName());
            }
        }
    }

    @Test
    void testDeletes() {
        List<Region> regions = Regions.newRegions(10, this.regionContext, this.hierarchyRepository.getAll().getFirst(), "Region ");
        for (Region r : regions) {
            r.addPermission(UUID.randomUUID(), 1);
            r.addPermission(UUID.randomUUID(), 1);

            RegionDataContainer dataContainer = r.getDataContainer();
            dataContainer.add(new RegionData("basic_data", "some_value"));
        }
        this.regionRepository.save(regions);

        this.regionRepository.delete(regions.subList(0, 5));

        ArgumentCaptor<List<Object>> inserts = ArgumentCaptor.forClass(List.class),
                                     updates = ArgumentCaptor.forClass(List.class),
                                     deletes = ArgumentCaptor.forClass(List.class);

        assertDoesNotThrow(() ->
            verify(this.dataBase, times(2)).write(inserts.capture(), updates.capture(), deletes.capture())
        );

        List<Object> insertValues = inserts.getValue(),
                     updateValues = updates.getValue(),
                     deleteValues = deletes.getValue();

        assertTrue(insertValues.isEmpty());
        assertFalse(updateValues.isEmpty());
        assertFalse(deleteValues.isEmpty());
        assertEquals(5, updateValues.size());  // 5 region rows marked as destroyed
        assertEquals(15, deleteValues.size()); // 2 permissions + 1 data per 5 regions. The region row is kept and marked as destroyed.

        for (Object o : updateValues) {
            DTO.Region regionDto = assertInstanceOf(DTO.Region.class, o);
            assertNotNull(regionDto);
            assertTrue(regionDto.destroyed());
        }

        for (Object o : deleteValues) {
            if (o instanceof DTO.Permission perm) {
                assertEquals(1, perm.level());
            } else if (o instanceof DTO.Data data) {
                assertEquals("basic_data", data.key());
            } else {
                fail("Unexpected DTO type in deletes: " + o.getClass().getName());
            }
        }
    }
}
