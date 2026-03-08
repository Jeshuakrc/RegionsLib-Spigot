package com.kntrel.mc.regionLib.util;

import com.kntrel.mc.regionLib.test.mock.MockWorld;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GridTest {

    private World world;

    @BeforeEach
    void setUp() {
        this.world = MockWorld.mockWorld();
    }

    @Test
    void iteratorTraversesRectangularGridInXThenZOrder() {
        Grid grid = new Grid(10, 20, 12, 21, this.world);

        List<Grid.Cell> cells = new ArrayList<>();
        for (Grid.Cell cell : grid) {
            cells.add(cell);
        }

        List<Grid.Cell> expected = List.of(
                new Grid.Cell(10, 20, this.world),
                new Grid.Cell(11, 20, this.world),
                new Grid.Cell(12, 20, this.world),
                new Grid.Cell(10, 21, this.world),
                new Grid.Cell(11, 21, this.world),
                new Grid.Cell(12, 21, this.world)
        );

        assertEquals(expected, cells);
    }

    @Test
    void iteratorSingleCellGridReturnsExactlyOneCell() {
        Grid grid = new Grid(3, -4, 3, -4, this.world);

        List<Grid.Cell> cells = grid.cellStream().toList();

        assertEquals(1, cells.size());
        assertEquals(new Grid.Cell(3, -4, this.world), cells.getFirst());
    }

    @Test
    void iteratorHasNextTransitionsCorrectly() {
        Grid grid = new Grid(0, 0, 1, 0, this.world);
        Iterator<Grid.Cell> iterator = grid.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(new Grid.Cell(0, 0, this.world), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(new Grid.Cell(1, 0, this.world), iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    void iteratorThrowsWhenExhausted() {
        Grid grid = new Grid(-1, -1, 0, 0, this.world);
        Iterator<Grid.Cell> iterator = grid.iterator();

        while (iterator.hasNext()) {
            iterator.next();
        }

        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void cellStreamAndIterationHaveSameOrder() {
        Grid grid = new Grid(-1, 2, 1, 3, this.world);

        List<Grid.Cell> iterated = new ArrayList<>();
        for (Grid.Cell cell : grid) {
            iterated.add(cell);
        }

        List<Grid.Cell> streamed = grid.cellStream().toList();

        assertEquals(iterated, streamed);
    }

    @Test
    void cellStreamAndCellSetContainAllCellsExactlyOnce() {
        Grid grid = new Grid(2, 4, 4, 5, this.world);

        List<Grid.Cell> streamCells = grid.cellStream().toList();
        Set<Grid.Cell> setCells = grid.cellSet();

        assertEquals(6, streamCells.size());
        assertEquals(6, setCells.size());
        assertEquals(setCells, Set.copyOf(streamCells));
    }

    @Test
    void cellSetContainsExpectedCoordinates() {
        Grid grid = new Grid(1, 1, 2, 2, this.world);

        Set<Grid.Cell> expected = Set.of(
                new Grid.Cell(1, 1, this.world),
                new Grid.Cell(2, 1, this.world),
                new Grid.Cell(1, 2, this.world),
                new Grid.Cell(2, 2, this.world)
        );

        assertEquals(expected, grid.cellSet());
    }

    @Test
    void containsAcceptsEveryIteratedCell() {
        Grid grid = new Grid(-2, 5, 0, 6, this.world);

        for (Grid.Cell cell : grid) {
            assertTrue(grid.contains(cell));
        }
    }

    @Test
    void containsChecksBoundsAndWorld() {
        World otherWorld = mock(World.class);
        when(otherWorld.getUID()).thenReturn(UUID.randomUUID());

        Grid grid = new Grid(-2, 5, 0, 6, this.world);

        assertTrue(grid.contains(new Grid.Cell(-2, 5, this.world)));
        assertTrue(grid.contains(new Grid.Cell(0, 6, this.world)));

        assertFalse(grid.contains(new Grid.Cell(-3, 5, this.world)));
        assertFalse(grid.contains(new Grid.Cell(1, 6, this.world)));
        assertFalse(grid.contains(new Grid.Cell(-2, 4, this.world)));
        assertFalse(grid.contains(new Grid.Cell(0, 7, this.world)));
        assertFalse(grid.contains(new Grid.Cell(-2, 5, otherWorld)));
    }

    @Test
    void cellEqualityUsesWorldUuid() {
        World sameUuidWorld = mock(World.class);
        UUID sharedUid = this.world.getUID();
        when(sameUuidWorld.getUID()).thenReturn(sharedUid);

        World differentWorld = mock(World.class);
        when(differentWorld.getUID()).thenReturn(UUID.randomUUID());

        Grid.Cell a = new Grid.Cell(7, 9, this.world);
        Grid.Cell b = new Grid.Cell(7, 9, sameUuidWorld);
        Grid.Cell c = new Grid.Cell(7, 9, differentWorld);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void cellSizeFactoriesAndConstantsAreConsistent() {
        assertEquals(5, Grid.CellSize.SIZE_32.shiftBy());
        assertEquals(32, Grid.CellSize.SIZE_32.getSize());
        assertEquals(Grid.CellSize.SIZE_32, Grid.CellSize.of(32));
        assertEquals(Grid.CellSize.SIZE_32, Grid.CellSize.ofTwoToThe(5));
        assertNotEquals(Grid.CellSize.SIZE_16, Grid.CellSize.SIZE_32);
    }

    @Test
    void cellSizeFactoryRejectsNonPowerOfTwo() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> Grid.CellSize.of(12)
        );
        assertTrue(error.getMessage().contains("power of two"));
    }
}