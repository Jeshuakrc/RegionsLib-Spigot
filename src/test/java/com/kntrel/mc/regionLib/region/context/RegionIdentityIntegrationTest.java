package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.persistence.sqlite.MockRegionContext;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.test.Regions;
import com.kntrel.mc.regionLib.test.mock.MockChunk;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegionIdentityIntegrationTest {

    private RegionContext context_;
    private RegionRepository mainRepository_;
    private RegionRepository rawRepository_;
    private HotRegionRepository hotRepository_;
    private Hierarchy hierarchy_;
    private World world_;


    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        this.context_ = MockRegionContext.mockContext();
        this.mainRepository_ = this.context_.getRegionRepository();
        this.rawRepository_ = getField(this.context_, "delegateRegionRepository_", RegionRepository.class);
        this.hotRepository_ = getField(this.context_, "hotRegionRepository_", HotRegionRepository.class);
        this.hierarchy_ = this.context_.getHierarchyRepository().getAll().getFirst();
        this.world_ = this.context_.getServer().getWorlds().getFirst();
    }


    @Test
    void testRepeatedMainReadsReuseCanonicalInstance() {
        Region created = Regions.newRegion(this.context_, this.hierarchy_, "Alpha");
        this.mainRepository_.save(created);

        Region first = this.mainRepository_.get(created.getId()).orElseThrow();
        Region second = this.mainRepository_.get(created.getId()).orElseThrow();

        assertSame(created, first);
        assertSame(first, second);
    }

    @Test
    void testMainAndHotRepositoriesShareSameInstance() {
        Region created = new Region(
                this.context_,
                chunkBox(0, 0),
                this.world_,
                "Hot Region",
                this.hierarchy_
        );
        this.mainRepository_.save(created);

        this.hotRepository_.handleChunkLoad(MockChunk.loadEvent(this.world_, 0, 0));

        Region fromMain = this.mainRepository_.get(created.getId()).orElseThrow();
        Region fromHot = this.context_.getHotRegionRepository().getAt(1, 1, 1, this.world_).getFirst();

        assertSame(created, fromMain);
        assertSame(fromMain, fromHot);
    }

    @Test
    void testSavingDetachedAliasPreservesCanonicalState() {
        Region created = Regions.newRegion(this.context_, this.hierarchy_, "Alias");
        this.mainRepository_.save(created);

        Region canonical = this.mainRepository_.get(created.getId()).orElseThrow();
        Region alias = this.rawRepository_.get(created.getId()).orElseThrow();
        assertNotSame(canonical, alias);

        UUID memberId = UUID.randomUUID();
        canonical.addPermission(memberId, 1);
        canonical.setRuleValue("greeting", "hello");
        canonical.save();

        alias.getDataContainer().add(new RegionData("coins", 5));
        alias.save();

        Region live = this.mainRepository_.get(created.getId()).orElseThrow();
        assertSame(canonical, live);
        assertTrue(live.getPermissions().stream().anyMatch(permission -> permission.getPlayerId().equals(memberId)));
        assertEquals("hello", live.getRuleValue("greeting").orElseThrow().toString());
        assertEquals(5, live.getDataContainer().get("coins").getAsInt());

        Region persisted = this.rawRepository_.get(created.getId()).orElseThrow();
        assertTrue(persisted.getPermissions().stream().anyMatch(permission -> permission.getPlayerId().equals(memberId)));
        assertEquals("hello", persisted.getRuleValue("greeting").orElseThrow().toString());
        assertEquals(5, persisted.getDataContainer().get("coins").getAsInt());

        assertTrue(alias.getPermissions().stream().anyMatch(permission -> permission.getPlayerId().equals(memberId)));
        assertEquals("hello", alias.getRuleValue("greeting").orElseThrow().toString());
        assertEquals(5, alias.getDataContainer().get("coins").getAsInt());
    }


    private static BoundingBox chunkBox(int chunkX, int chunkZ) {
        return new BoundingBox(
                chunkX << Constants.CHUNK_SHIFT,
                0,
                chunkZ << Constants.CHUNK_SHIFT,
                (chunkX + 1) << Constants.CHUNK_SHIFT,
                256,
                (chunkZ + 1) << Constants.CHUNK_SHIFT
        );
    }
    private static <T> T getField(Object target, String name, Class<T> type) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
