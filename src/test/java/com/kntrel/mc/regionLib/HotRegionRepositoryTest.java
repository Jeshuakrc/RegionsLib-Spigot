package com.kntrel.mc.regionLib;

import com.kntrel.mc.regionLib.persistence.sqlite.MockRegionContext;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.test.mock.MockChunk;
import com.kntrel.mc.regionLib.test.mock.MockWorld;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HotRegionRepositoryTest {

    //ASSETS
    private RegionContext regionContext;
    private World world;
    private HotRegionRepository repository;
    private RegionRepository delegate;


    //SETUP
    @BeforeEach void setup() {
        this.regionContext = MockRegionContext.mockContext();
        this.world = MockWorld.mockWorld();
        this.delegate = spy(this.regionContext.getRegionRepository());
        this.repository = new HotRegionRepository(delegate, HotRegionRepository.GridSize.SIZE_32);
    }


    //TESTS
    @Test
    void testReturnsOnlyHotRegionsAfterLoadAndUnload() {
        Region regionA = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "Region A");
        Region regionB = regionInChunks(4, 4, 4, 4, this.regionContext, this.world, "Region B");

        this.repository.save(regionA, regionB);

        assertEquals(2, this.delegate.getAll().size());
        assertTrue(this.repository.getAll().isEmpty());
        assertFalse(this.delegate.getAll().isEmpty());
        assertEquals(2, this.delegate.getAll().size());

        loadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "Region A");

        loadChunk(4, 4);
        assertRegionNames(this.repository.getAll(), "Region A", "Region B");

        unloadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "Region B");
    }

    @Test
    void testCellLoadAndUnloadAcrossChunks() {
        Region region = regionInChunks(0, 0, 1, 1, this.regionContext, this.world, "Cell Region");
        this.repository.save(region);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "Cell Region");

        loadChunk(1, 1);
        assertRegionNames(this.repository.getAll(), "Cell Region");

        unloadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "Cell Region");

        unloadChunk(1, 1);
        assertTrue(this.repository.getAll().isEmpty());
    }

    @Test
    void testRegionEvictedOnDestroy() {
        Region region = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "Evict");
        this.repository.save(region);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "Evict");

        this.repository.delete(region);
        assertTrue(this.repository.getAll().isEmpty());
    }

    @Test
    void testColdRegionsInLoadedCellRemainHiddenUntilChunkLoads() {
        Region hotRegion = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "Hot");
        Region coldRegion = regionInChunks(1, 0, 1, 0, this.regionContext, this.world, "Cold");
        this.repository.save(hotRegion, coldRegion);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "Hot");

        loadChunk(1, 0);
        assertRegionNames(this.repository.getAll(), "Cold", "Hot");
    }




    //HELPERS
    private void loadChunk(int x, int z) {
        this.repository.onChunkLoad(MockChunk.loadEvent(this.world, x, z));
    }

    private void unloadChunk(int x, int z) {
        Chunk chunk = this.world.getChunkAt(x, z);
        this.repository.onChunkUnload(MockChunk.unloadEvent(chunk));
    }

    private static void assertRegionNames(List<Region> regions, String... expectedNames) {
        List<String> actualNames = regions.stream()
                .map(Region::getName)
                .sorted()
                .toList();
        List<String> expected = Stream.of(expectedNames)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        assertEquals(expected, actualNames);
    }

    private static Region regionInChunks(int x1, int z1, int x2, int z2, RegionContext ctx, World world, String name) {

        BoundingBox bb = new BoundingBox(
                x1 << Constants.CHUNK_SHIFT,
                0,
                z1 << Constants.CHUNK_SHIFT,
                (x2 + 1) << Constants.CHUNK_SHIFT,
                256,
                (z2 + 1) << Constants.CHUNK_SHIFT
        );
        return new Region(
                ctx,
                bb,
                world,
                name,
                ctx.getHierarchyRepository().getAll().getFirst()
        );
    }
}
