package com.kntrel.mc.regionLib.test.mock;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;

public class MockChunk implements Chunk {

    public static ChunkLoadEvent loadEvent(World world, int chunkX, int chunkZ) {
        MockChunk chunk = new MockChunk(chunkX, chunkZ, world);
        chunk.setLoaded();
        return new ChunkLoadEvent(chunk, false);
    }
    public static ChunkUnloadEvent unloadEvent(Chunk chunk) {
        if (chunk instanceof MockChunk c) {
            c.setUnloaded();
        }
        return new ChunkUnloadEvent(chunk, false);
    }


    //FIELDS
    private final int chunkX_;
    private final int chunkZ_;
    private final World world_;
    private boolean loaded_ = false;


    //CONSTRUCTORS
    public MockChunk(int chunkX, int chunkZ, World world) {
        this.chunkX_ = chunkX;
        this.chunkZ_ = chunkZ;
        this.world_ = world;
    }


    //SETTERS
    public void setLoaded() {
        this.loaded_ = true;
    }
    public void setUnloaded() {
        this.loaded_ = false;
    }


    //IMPLEMENTATION
    @Override public int getX() {
        return this.chunkX_;
    }
    @Override public int getZ() {
        return this.chunkZ_;
    }
    @NotNull @Override public World getWorld() {
        return this.world_;
    }
    @NotNull @Override public Block getBlock(int i, int i1, int i2) {
        unimplemented();
        return null;
    }
    @NotNull @Override public ChunkSnapshot getChunkSnapshot() {
        unimplemented();
        return null;
    }
    @NotNull @Override public ChunkSnapshot getChunkSnapshot(boolean b, boolean b1, boolean b2) {
        unimplemented();
        return null;
    }
    @Override public boolean isEntitiesLoaded() {
        unimplemented();
        return false;
    }
    @NotNull @Override public Entity[] getEntities() {
        unimplemented();
        return new Entity[0];
    }
    @NotNull @Override public BlockState[] getTileEntities() {
        unimplemented();
        return new BlockState[0];
    }
    @Override public boolean isGenerated() {
        unimplemented();
        return false;
    }
    @Override public boolean isLoaded() {
        unimplemented();
        return false;
    }
    @Override public boolean load(boolean b) {
        unimplemented();
        return false;
    }
    @Override public boolean load() {
        unimplemented();
        return false;
    }
    @Override public boolean unload(boolean b) {
        unimplemented();
        return false;
    }
    @Override public boolean unload() {
        unimplemented();
        return false;
    }
    @Override public boolean isSlimeChunk() {
        unimplemented();
        return false;
    }
    @Override public boolean isForceLoaded() {
        unimplemented();
        return false;
    }
    @Override public void setForceLoaded(boolean b) {
        unimplemented();
    }
    @Override public boolean addPluginChunkTicket(@NotNull Plugin plugin) {
        unimplemented();
        return false;
    }
    @Override public boolean removePluginChunkTicket(@NotNull Plugin plugin) {
        unimplemented();
        return false;
    }
    @NotNull @Override public Collection<Plugin> getPluginChunkTickets() {
        unimplemented();
        return null;
    }
    @Override public long getInhabitedTime() {
        unimplemented();
        return 0;
    }
    @Override public void setInhabitedTime(long l) {
        unimplemented();
    }
    @Override public boolean contains(@NotNull BlockData blockData) {
        unimplemented();
        return false;
    }
    @Override public boolean contains(@NotNull Biome biome) {
        unimplemented();
        return false;
    }
    @NotNull @Override public LoadLevel getLoadLevel() {
        unimplemented();
        return null;
    }
    @NotNull @Override public Collection<GeneratedStructure> getStructures() {
        unimplemented();
        return null;
    }
    @NotNull @Override public Collection<GeneratedStructure> getStructures(@NotNull Structure structure) {
        unimplemented();
        return null;
    }
    @NotNull @Override public Collection<Player> getPlayersSeeingChunk() {
        unimplemented();
        return null;
    }
    @NotNull @Override public PersistentDataContainer getPersistentDataContainer() {
        unimplemented();
        return null;
    }


    //HELPERS
    private static void unimplemented() {
        throw new UnsupportedOperationException("Not implemented in MockChunk");
    }
}
