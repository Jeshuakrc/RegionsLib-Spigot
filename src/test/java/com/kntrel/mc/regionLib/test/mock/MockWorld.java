package com.kntrel.mc.regionLib.test.mock;

import org.bukkit.World;

import static org.mockito.Mockito.*;

public final class MockWorld {

    private MockWorld() {}

    private static World MOCK_WORLD = null;

    public static World mockWorld() {
        if (MOCK_WORLD != null) {
            return MOCK_WORLD;
        }

        MOCK_WORLD = mock(World.class);
        when(MOCK_WORLD.getName()).thenReturn("world");

        return MOCK_WORLD;
    }

}
