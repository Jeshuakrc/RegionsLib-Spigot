package com.kntrel.mc.regionLib.test.mock;

import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

public final class MockWorld {

    private MockWorld() {}

    private static final UUID WORLD_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static World mockWorld() {

        World mock = mock(World.class);
        Map<Long, MockChunk> chunks = new HashMap<>();
        when(mock.getName()).thenReturn("world");
        when(mock.getUID()).thenReturn(WORLD_UUID);
        when(mock.getChunkAt(anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    int x = invocation.getArgument(0);
                    int z = invocation.getArgument(1);
                    long key = (((long) x) << 32) ^ (z & 0xffffffffL);
                    return chunks.computeIfAbsent(key, k -> new MockChunk(x, z, mock));
                });
        when(mock.unloadChunk(any(Chunk.class))).thenAnswer(invocation -> {
            Chunk chunk = invocation.getArgument(0);
            if (chunk instanceof MockChunk c) {
                c.setUnloaded();
                return true;
            }
            return false;
        });
        when(mock.isChunkLoaded(anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    int x = invocation.getArgument(0);
                    int z = invocation.getArgument(1);
                    long key = (((long) x) << 32) ^ (z & 0xffffffffL);
                    MockChunk chunk = chunks.get(key);
                    return chunk != null && chunk.isLoaded();
                });

        return mock;
    }

}
