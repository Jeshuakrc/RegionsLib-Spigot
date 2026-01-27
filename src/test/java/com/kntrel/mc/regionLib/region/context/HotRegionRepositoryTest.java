package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.cache.RegionCache;
import com.kntrel.mc.regionLib.persistence.sqlite.MockRegionContext;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.test.mock.MockChunk;
import com.kntrel.mc.regionLib.util.Grid;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HotRegionRepositoryTest {

    //CONSTANTS
    private static final Grid.CellSize GRID_SIZE = Grid.CellSize.SIZE_32;


    //ASSETS
    private RegionContext regionContext;
    private World world;
    private HotRegionRepository repository;
    private RegionRepository delegate;


    //SETUP
    @BeforeEach void setup() {
        this.regionContext = MockRegionContext.mockContext();
        this.delegate = spy(this.regionContext.getRegionRepository());
        this.repository = new HotRegionRepository(new RegionCache(), this.delegate, GRID_SIZE);
        this.world = this.regionContext.getServer().getWorlds().getFirst();
    }


    //TESTS
    @Test void testReturnsOnlyHotRegionsAfterLoadAndUnload() {
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
    @Test void testCellLoadAndUnloadAcrossChunks() {
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
    @Test void testRegionEvictedOnDestroy() {
        Region region = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "Evict");
        this.repository.save(region);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "Evict");

        region.destroy();
        this.repository.save(region);
        assertTrue(this.repository.getAll().isEmpty());
    }
    @Test void testColdRegionsInLoadedCellRemainHiddenUntilChunkLoads() {
        Region hotRegion = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "HotReg");
        Region coldRegion = regionInChunks(1, 0, 1, 0, this.regionContext, this.world, "ColdReg");
        this.repository.save(hotRegion, coldRegion);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "HotReg");

        loadChunk(1, 0);
        assertRegionNames(this.repository.getAll(), "ColdReg", "HotReg");
    }
    @Test void testRegionResizeMakesHotWhenTouchingLoadedChunk() {
        Region region = regionInChunks(2, 2, 2, 2, this.regionContext, this.world, "Resize Hot");
        this.repository.save(region);

        loadChunk(0, 0);
        assertTrue(this.repository.getAll().isEmpty());

        resizeRegionToChunks(region, 0, 0, 2, 2);
        this.repository.save(region);
        assertRegionNames(this.repository.getAll(), "Resize Hot");
    }
    @Test void testRegionResizeCoolsWhenLeavingLoadedChunk() {
        Region region = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "Resize Cold");
        this.repository.save(region);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getAll(), "Resize Cold");

        resizeRegionToChunks(region, 3, 3, 4, 5);
        this.repository.save(region);
        assertTrue(this.repository.getAll().isEmpty());
    }
    @Test void testWarmRegionsIncludeColdRegionsInLoadedCell() {
        Region hotRegion = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "HotReg");
        Region coldRegion = regionInChunks(1, 0, 1, 0, this.regionContext, this.world, "ColdReg");
        this.repository.save(hotRegion, coldRegion);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getWarmRegions(), "ColdReg", "HotReg");
        assertRegionNames(this.repository.getAll(), "HotReg");
    }
    @Test void testWarmRegionUnloadAfterLastChunkLeavesCell() {
        Region region = regionInChunks(0, 0, 1, 1, this.regionContext, this.world, "Warm Region");
        this.repository.save(region);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getWarmRegions(), "Warm Region");

        loadChunk(1, 1);
        assertRegionNames(this.repository.getWarmRegions(), "Warm Region");

        unloadChunk(0, 0);
        assertRegionNames(this.repository.getWarmRegions(), "Warm Region");

        unloadChunk(1, 1);
        assertTrue(this.repository.getWarmRegions().isEmpty());
    }
    @Test void testWarmRegionsUpdateOnResizeIntoLoadedCell() {
        Region region = regionInChunks(2, 2, 2, 2, this.regionContext, this.world, "Warm Resize In");
        this.repository.save(region);

        loadChunk(0, 0);
        assertTrue(this.repository.getWarmRegions().isEmpty());

        resizeRegionToChunks(region, 0, 0, 2, 2);
        this.repository.save(region);
        assertRegionNames(this.repository.getWarmRegions(), "Warm Resize In");
    }
    @Test void testWarmRegionsUpdateOnResizeOutOfLoadedCell() {
        Region region = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "Warm Resize Out");
        this.repository.save(region);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getWarmRegions(), "Warm Resize Out");

        resizeRegionToChunks(region, 3, 3, 4, 5);
        this.repository.save(region);
        assertTrue(this.repository.getWarmRegions().isEmpty());
    }
    @Test void testWarmAndHotRegionsAcrossMultipleCells() {
        Region regionA = regionInChunks(0, 0, 0, 0, this.regionContext, this.world, "Region A");
        Region regionB = regionInChunks(1, 0, 1, 0, this.regionContext, this.world, "Region B");
        Region regionC = regionInChunks(3, 3, 3, 3, this.regionContext, this.world, "Region C");
        Region regionD = regionInChunks(6, 6, 7, 7, this.regionContext, this.world, "Region D");
        this.repository.save(regionA, regionB, regionC, regionD);

        loadChunk(0, 0);
        assertRegionNames(this.repository.getWarmRegions(), "Region A", "Region B");
        assertRegionNames(this.repository.getAll(), "Region A");

        loadChunk(1, 0);
        assertRegionNames(this.repository.getWarmRegions(), "Region A", "Region B");
        assertRegionNames(this.repository.getAll(), "Region A", "Region B");

        loadChunk(6, 6);
        assertRegionNames(this.repository.getWarmRegions(), "Region A", "Region B", "Region D");
        assertRegionNames(this.repository.getAll(), "Region A", "Region B", "Region D");

        unloadChunk(0, 0);
        assertRegionNames(this.repository.getWarmRegions(), "Region A", "Region B", "Region D");
        assertRegionNames(this.repository.getAll(), "Region B", "Region D");

        unloadChunk(1, 0);
        assertRegionNames(this.repository.getWarmRegions(), "Region D");
        assertRegionNames(this.repository.getAll(), "Region D");

        unloadChunk(6, 6);
        assertTrue(this.repository.getWarmRegions().isEmpty());
        assertTrue(this.repository.getAll().isEmpty());
    }
    @Test void testWarmAndHotRegionsAcrossMultipleCells2() {
        int cellSizeChunks = 1 << (GRID_SIZE.shiftBy() - Constants.CHUNK_SHIFT);
        int chunksInCell = cellSizeChunks * cellSizeChunks;
        for (int x = 0; x < cellSizeChunks * 2; x++) {
            for (int z = 0; z < cellSizeChunks * 2; z++) {
                Region region = regionInChunks(x, z, x, z, this.regionContext, this.world, "Region " + x + "," + z);
                this.repository.save(region);
            }
        }

        assertTrue(this.repository.getWarmRegions().isEmpty());

        loadChunk(0, 0);
        assertEquals(chunksInCell, this.repository.getWarmRegions().size());
        assertEquals(1, this.repository.getAll().size());
        assertRegionNames(this.repository.getAll(), "Region 0,0");

        loadChunk(1, 0);
        assertEquals(cellSizeChunks * cellSizeChunks, this.repository.getWarmRegions().size());
        assertEquals(2, this.repository.getAll().size());
        assertRegionNames(this.repository.getAll(), "Region 0,0", "Region 1,0");

        loadChunk(cellSizeChunks, 0);
        assertEquals(chunksInCell * 2, this.repository.getWarmRegions().size());
        assertEquals(3, this.repository.getAll().size());
        assertRegionNames(this.repository.getAll(), "Region 0,0", "Region 1,0", "Region " + cellSizeChunks + ",0");

        unloadChunk(1, 0);
        assertEquals(chunksInCell * 2, this.repository.getWarmRegions().size());
        assertEquals(2, this.repository.getAll().size());
        assertRegionNames(this.repository.getAll(), "Region 0,0", "Region " + cellSizeChunks + ",0");

        loadChunk(0, cellSizeChunks);
        assertEquals(chunksInCell * 3, this.repository.getWarmRegions().size());
        assertEquals(3, this.repository.getAll().size());
        assertRegionNames(this.repository.getAll(), "Region 0,0", "Region " + cellSizeChunks + ",0", "Region 0," + cellSizeChunks);

        loadChunk(cellSizeChunks, cellSizeChunks);
        assertEquals(chunksInCell * 4, this.repository.getWarmRegions().size());
        assertEquals(4, this.repository.getAll().size());
        assertRegionNames(this.repository.getAll(), "Region 0,0", "Region " + cellSizeChunks + ",0", "Region 0," + cellSizeChunks, "Region " + cellSizeChunks + "," + cellSizeChunks);

        List<Region> queried = this.repository.where().at(0.5, 1, 0.5, this.world).get();
        assertEquals(1, queried.size());
        assertRegionNames(queried, "Region 0,0");

        unloadChunk(0, 0);
        assertEquals(chunksInCell * 3, this.repository.getWarmRegions().size());
        assertEquals(3, this.repository.getAll().size());
        assertRegionNames(this.repository.getAll(), "Region " + cellSizeChunks + ",0", "Region 0," + cellSizeChunks, "Region " + cellSizeChunks + "," + cellSizeChunks);

        queried = this.repository.where().at(0.5, 1, 0.5, this.world).get();
        assertTrue(queried.isEmpty());

        queried = this.repository.where().at((cellSizeChunks << Constants.CHUNK_SHIFT) + 0.5, 1, 0.5, this.world).get();
        assertEquals(1, queried.size());
        assertRegionNames(queried, "Region " + cellSizeChunks + ",0");
    }



    //HELPERS
    private void loadChunk(int x, int z) {
        this.repository.handleChunkLoad(MockChunk.loadEvent(this.world, x, z));
    }
    private void unloadChunk(int x, int z) {
        Chunk chunk = this.world.getChunkAt(x, z);
        this.repository.handleChunkUnload(MockChunk.unloadEvent(chunk));
    }
    private static void assertRegionNames(List<Region> regions, String... expectedNames) {
        List<String> actualNames = regions.stream()
                .map(Region::getName)
                .sorted()
                .toList();
        List<String> expected = List.of(expectedNames).stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        assertEquals(expected, actualNames);
    }
    private static void resizeRegionToChunks(Region region, int x1, int z1, int x2, int z2) {
        region.resize(boundingBoxForChunks(x1, z1, x2, z2));
    }
    private static BoundingBox boundingBoxForChunks(int x1, int z1, int x2, int z2) {
        return new BoundingBox(
                x1 << Constants.CHUNK_SHIFT,
                0,
                z1 << Constants.CHUNK_SHIFT,
                (x2 + 1) << Constants.CHUNK_SHIFT,
                256,
                (z2 + 1) << Constants.CHUNK_SHIFT
        );
    }
    private static Region regionInChunks(int x1, int z1, int x2, int z2, RegionContext ctx, World world, String name) {
        return new Region(
                ctx,
                boundingBoxForChunks(x1, z1, x2, z2),
                world,
                name,
                ctx.getHierarchyRepository().getAll().getFirst()
        );
    }
}
