package com.kntrel.mc.regionLib;

import com.kntrel.mc.regionLib.persistence.sqlite.MockRegionContext;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.test.mock.MockWorld;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
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




    //HELPERS
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
