package com.kntrel.mc.regionLib.persistence.sqlite;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.test.Regions;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.mock.MockServer;
import com.kntrel.util.tuple.Pair;
import com.kntrel.util.tuple.Triplet;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        this.hierarchyRepository = MockHierarchyRepository.ofSingle("hierarchy", 3);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(this.server);
        this.regionContext = new RegionContext(
                RegionContextConfig.build().withCacheCapacity(10).end(),
                plugin,
                ctx -> {
                    this.queryParser = new QueryParser(ctx);
                    this.regionRepository = new SQLiteRegionRepository(ctx, this.dataBase, this.queryParser, this.executorService);
                    return this.regionRepository;
                },
                ctx -> this.hierarchyRepository
        );
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

    @Test
    void testMixedWrites() {
        Hierarchy hierarchy = this.hierarchyRepository.getAll().getFirst();
        Region region = Regions.newRegion(this.regionContext, hierarchy, "Test Region");
        
        ArgumentCaptor<List<Object>> inserts = ArgumentCaptor.forClass(List.class),
                                     updates = ArgumentCaptor.forClass(List.class),
                                     deletes = ArgumentCaptor.forClass(List.class);
        
        // Test region insertion
        this.regionRepository.save(region);
        
        assertDoesNotThrow(() ->
            verify(this.dataBase).write(inserts.capture(), updates.capture(), deletes.capture())
        );
        
        List<Object> insertValues = inserts.getValue(),
                     updateValues = updates.getValue(),
                     deleteValues = deletes.getValue();
        
        assertTrue(updateValues.isEmpty());
        assertTrue(deleteValues.isEmpty());
        assertEquals(1, insertValues.size());               // 1 region inserted
        assertInstanceOf(DTO.Region.class, insertValues.getFirst());
        
        // Test permission insertion
        Permission permission = new Permission(UUID.randomUUID(), region, 1);
        region.addPermission(permission);
        this.regionRepository.save(region);
        
        assertDoesNotThrow(() ->
            verify(this.dataBase, times(2)).write(inserts.capture(), updates.capture(), deletes.capture())
        );
        
        insertValues = inserts.getValue();
        updateValues = updates.getValue();
        deleteValues = deletes.getValue();
        
        assertTrue(updateValues.isEmpty());
        assertTrue(deleteValues.isEmpty());
        assertEquals(1, insertValues.size());               // 1 permission inserted
        DTO.Permission perm = assertInstanceOf(DTO.Permission.class, insertValues.get(0));
        assertEquals(1, perm.level());
        
        // Test permission update
        region.removePermission(permission);
        permission = new Permission(permission.getPlayerId(), region, 2);
        region.addPermission(permission);
        this.regionRepository.save(region);
        
        assertDoesNotThrow(() ->
            verify(this.dataBase, times(3)).write(inserts.capture(), updates.capture(), deletes.capture())
        );
        
        insertValues = inserts.getValue();
        updateValues = updates.getValue();
        deleteValues = deletes.getValue();
        
        assertTrue(insertValues.isEmpty());
        assertTrue(deleteValues.isEmpty());
        assertEquals(1, updateValues.size());               // 1 permission updated
        perm = assertInstanceOf(DTO.Permission.class, updateValues.getFirst());
        assertEquals(2, perm.level());
        
        // Test data insertion
        RegionDataContainer dataContainer = region.getDataContainer();
        dataContainer.add(new RegionData("key1", "value1"));
        dataContainer.add(new RegionData("key2", "value2"));
        dataContainer.add(new RegionData("key3", "value3"));
        this.regionRepository.save(region);
        
        assertDoesNotThrow(() ->
            verify(this.dataBase, times(4)).write(inserts.capture(), updates.capture(), deletes.capture())
        );
        
        insertValues = inserts.getValue();
        updateValues = updates.getValue();
        deleteValues = deletes.getValue();
        
        assertTrue(updateValues.isEmpty());
        assertTrue(deleteValues.isEmpty());
        assertEquals(3, insertValues.size());               // 3 data entries inserted
        for (Object o : insertValues) {
            assertInstanceOf(DTO.Data.class, o);
        }
        
        // Test Remove and change data entries
        dataContainer.remove("key1");
        dataContainer.get("key2").setValue("value2_updated");
        this.regionRepository.save(List.of(region));
        
        assertDoesNotThrow(() ->
            verify(this.dataBase, times(5)).write(inserts.capture(), updates.capture(), deletes.capture())
        );
        
        insertValues = inserts.getValue();
        updateValues = updates.getValue();
        deleteValues = deletes.getValue();
        
        assertTrue(insertValues.isEmpty());
        assertEquals(1, updateValues.size());               // 1 data entry updated
        assertEquals(1, deleteValues.size());               // 1 data entry deleted
        assertInstanceOf(DTO.Data.class, updateValues.getFirst());
        assertInstanceOf(DTO.Data.class, deleteValues.getFirst());
    }

    @Test
    void testQuery() {
        Hierarchy hierarchy = this.hierarchyRepository.getAll().getFirst();
        Map<String, Region> regions = Stream.of(
                Regions.newRegion(this.regionContext, hierarchy, "Alpha"),
                Regions.newRegion(this.regionContext, hierarchy, "Beta"),
                Regions.newRegion(this.regionContext, hierarchy, "Gamma"),
                Regions.newRegion(this.regionContext, hierarchy, "Theta"),
                Regions.newRegion(this.regionContext, hierarchy, "Spawn"),
                Regions.newRegion(this.regionContext, hierarchy, "Toilets"),
                Regions.newRegion(this.regionContext, hierarchy, "Admin Area"),
                Regions.newRegion(this.regionContext, hierarchy, "Lobby")
        ).collect(Collectors.toMap(Region::getName, r -> r));

        Set<String> disabled = Set.of("Alpha", "Theta", "Toilets");
        regions.values().stream().filter(r -> disabled.contains(r.getName()))
                .forEach(r -> r.enabled(false));

        regions.get("Spawn").getDataContainer().add(new RegionData("welcome_message", "Welcome to the server!"));
        regions.get("Lobby").destroy();
        regions.get("Beta").resize(10<<4, 0, 20<<4, (10<<4) + 1, 1, (20<<4) + 1); // Chunk (10,20)

        this.regionRepository.save(regions.values().stream().toList());

        List<Region> result = this.regionRepository.where().isFalse(RegionField.ENABLED).get();
        assertEquals(3, result.size());
        for (Region r : result) {
            assertFalse(r.isEnabled());
            assertTrue(disabled.contains(r.getName()));
        }

        result = this.regionRepository.where().nameIs("Admin Area").get();
        assertEquals(1, result.size());
        assertEquals("Admin Area", result.getFirst().getName());

        result = this.regionRepository.where().dataValueIs("welcome_message", new JsonPrimitive("Welcome to the server!")).get();
        assertEquals(1, result.size());
        assertEquals("Spawn", result.getFirst().getName());

        result = this.regionRepository.getAll();
        assertEquals(7, result.size());

        result = this.regionRepository.where().includeDestroyed().get();
        assertEquals(8, result.size());

        result = this.regionRepository.where()
                    .isEnabled()
                .or()
                    .nameIs("Toilets")
                .get();
        assertEquals(5, result.size());

        result = this.regionRepository.where()
                .inChunk(0, 0, result.getFirst().getWorld())
                .get();
        assertEquals(6, result.size());

        result = this.regionRepository.where()
                .inChunk(0, 0, result.getFirst().getWorld())
                .limit(4)
                .get();
        assertEquals(4, result.size());

        result = this.regionRepository.where()
                .inChunk(0, 0, result.getFirst().getWorld())
                .includeDestroyed()
                .get();
        assertEquals(7, result.size());

        result = this.regionRepository.where()
                .inChunk(0, 0, result.getFirst().getWorld())
                .isEnabled()
                .get();
        assertEquals(3, result.size());
    }

    @Test
    void testFieldProjectionContract() {
        Hierarchy hierarchy = this.hierarchyRepository.getAll().getFirst();
        Region alpha = Regions.newRegion(this.regionContext, hierarchy, "Alpha");
        alpha.enabled(false);
        Region beta = Regions.newRegion(this.regionContext, hierarchy, "Beta");
        Region gamma = Regions.newRegion(this.regionContext, hierarchy, "Gamma");

        this.regionRepository.save(alpha, beta, gamma);

        RegionRepository repo = this.regionContext.getRegionRepository();
        Query query = repo.where()
                .orderBy(RegionField.ID)
                .asQuery();

        List<Long> ids = repo.get(query, RegionField.ID);
        assertEquals(List.of(alpha.getId(), beta.getId(), gamma.getId()), ids);

        List<Pair<Long, String>> idNames = repo.get(query, RegionField.ID, RegionField.NAME);
        assertEquals(3, idNames.size());
        assertEquals(alpha.getId(), idNames.get(0).first());
        assertEquals("Alpha", idNames.get(0).second());
        assertEquals(beta.getId(), idNames.get(1).first());
        assertEquals("Beta", idNames.get(1).second());

        List<Triplet<Long, String, Boolean>> triples = repo.get(query, RegionField.ID, RegionField.NAME, RegionField.ENABLED);
        assertEquals(3, triples.size());
        assertEquals(alpha.getId(), triples.get(0).first());
        assertEquals("Alpha", triples.get(0).second());
        assertFalse(triples.get(0).third());
        assertTrue(triples.get(1).third());

        List<Object[]> rows = repo.get(query, new RegionField<?>[] { RegionField.NAME, RegionField.WORLD });
        assertEquals(3, rows.size());
        assertArrayEquals(new Object[] { "Alpha", alpha.getWorld() }, rows.get(0));
        assertArrayEquals(new Object[] { "Beta", beta.getWorld() }, rows.get(1));
    }

    @Test
    void testIsInConditionQuery() {
        Hierarchy hierarchy = this.hierarchyRepository.getAll().getFirst();
        Region alpha = Regions.newRegion(this.regionContext, hierarchy, "Alpha");
        Region beta = Regions.newRegion(this.regionContext, hierarchy, "Beta");
        Region gamma = Regions.newRegion(this.regionContext, hierarchy, "Gamma");

        this.regionRepository.save(alpha, beta, gamma);

        List<Region> result = this.regionRepository.where()
                .isIn(RegionField.NAME, List.of("Alpha", "Gamma"))
                .orderBy(RegionField.NAME)
                .get();

        assertEquals(2, result.size());
        assertEquals("Alpha", result.get(0).getName());
        assertEquals("Gamma", result.get(1).getName());
    }

    @Test
    void testCachedSaves() {
        RegionRepository repo = new RegionRepository() {
            @Override
            public void save(Region... regions) {
                SQLiteRegionRepositoryTest.this.regionRepository.save(regions);
                for (Region reg : regions) {
                    SQLiteRegionRepositoryTest.this.regionContext.getCache().put(reg);
                }
            }

            @Override
            public List<Region> get(Query query) {
                return SQLiteRegionRepositoryTest.this.regionRepository.get(query);
            }
        };

        //Injected an ConcurrentLRUCache with capacity = 10

        //Filling th cache up
        Map<String, Region> regs = Regions.newRegions(
                this.regionContext,
                this.hierarchyRepository.getAll().getFirst(),
                "region1",
                "region2",
                "region3",
                "region4",
                "region5",
                "region6",
                "region7",
                "region8",
                "region9",
                "region10"
        ).stream().collect(Collectors.toMap(Region::getName, r -> r));
        repo.save(regs.values());

        // Re-saving regions shouldn't cause database calls
        repo.save(regs.get("region8"));
        repo.save(regs.get("region3"));
        repo.save(regs.get("region1"));    // region1 was next to be evicted but this call resents its LRU value
        assertDoesNotThrow(() ->
            verify(this.dataBase, never()).query(anyString(), any(), anyInt())
        );

        // Adding a new region should evict the least recently used region (region2)
        Region newReg = Regions.newRegion(this.regionContext, this.hierarchyRepository.getAll().getFirst(), "Region11");
        repo.save(newReg);
        assertDoesNotThrow(() ->
                verify(this.dataBase, never()).query(anyString(), any(), anyInt())
        );

        // Accessing any other regions shouldn't cause reads
        repo.save(regs.get("region5"));
        repo.save(regs.get("region4"));
        repo.save(newReg);
        assertDoesNotThrow(() ->
                verify(this.dataBase, never()).query(anyString(), any(), anyInt())
        );

        // Accessing region2 should cause a read from the database since it was evicted
        repo.save(regs.get("region2"));
        assertDoesNotThrow(() ->
                verify(this.dataBase, atLeastOnce()).query(anyString(), any(), anyInt())
        );
    }

    @Test
    void testHighVolumeStressQuery() {
        Hierarchy hierarchy = this.hierarchyRepository.getAll().getFirst();
        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < 400_000; i++) {
            Region reg = Regions.newRegion(this.regionContext, hierarchy, "reg_" + i);
            if (i % 2 == 0) { reg.enabled(false); }
            regions.add(reg);
        }

        assertDoesNotThrow(() -> this.regionRepository.save(regions));
        List<Region> fetched = assertDoesNotThrow(() -> this.regionRepository.where().orderBy(RegionField.ID).get());
        assertEquals(regions.size(), fetched.size());

        Collections.sort(regions);
        Iterator<Region> ri = regions.iterator(), fi = fetched.iterator();
        while (ri.hasNext()) {
            Region rExpected = ri.next();
            Region rFetched = fi.next();
            assertEquals(rExpected.getId(), rFetched.getId());
            assertEquals(rExpected.getName(), rFetched.getName());
            assertEquals(rExpected.isEnabled(), rFetched.isEnabled());
        }

        fetched = assertDoesNotThrow(() -> this.regionRepository.where().isEnabled().orderBy(RegionField.ID).get());
        ri = regions.iterator(); fi = fetched.iterator();
        while (ri.hasNext()) {
            Region rExpected = ri.next();
            if (!rExpected.isEnabled()) { continue; }
            Region rFetched = fi.next();
            assertEquals(rExpected.getId(), rFetched.getId());
            assertEquals(rExpected.getName(), rFetched.getName());
            assertEquals(rExpected.isEnabled(), rFetched.isEnabled());
        }

        fetched = assertDoesNotThrow(() -> this.regionRepository
                .where(Condition.OR(
                        Condition.between(RegionField.ID, 101L, 2000L),
                        Condition.between(RegionField.ID, 100_001L, 200_000L),
                        Condition.isTrue(RegionField.ENABLED)
                ))
                .orderBy(RegionField.ID)
                .get()
        );
        ri = regions.iterator(); fi = fetched.iterator();
        while (ri.hasNext()) {
            Region rExpected = ri.next();
            boolean inRange = (rExpected.getId() >= 101L && rExpected.getId() <= 2000L)
                           || (rExpected.getId() >= 100_001L && rExpected.getId() <= 200_000L)
                           || rExpected.isEnabled();
            if (!inRange) { continue; }
            Region rFetched = fi.next();
            assertEquals(rExpected.getId(), rFetched.getId());
            assertEquals(rExpected.getName(), rFetched.getName());
            assertEquals(rExpected.isEnabled(), rFetched.isEnabled());
        }
    }
}
