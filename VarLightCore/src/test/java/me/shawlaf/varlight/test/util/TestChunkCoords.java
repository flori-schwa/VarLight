package me.shawlaf.varlight.test.util;

import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestChunkCoords {

    @Test
    public void testChunkCoords() {
        doTest(new ChunkCoords(1, 1), 1, 1, 0, 0, 1, 1);
        doTest(new ChunkCoords(1, -1), 1, -1, 0, -1, 1, 31);
        doTest(new ChunkCoords(-1, -1), -1, -1, -1, -1, 31, 31);
        doTest(new ChunkCoords(-1, 1), -1, 1, -1, 0, 31, 1);
    }

    private void doTest(
            ChunkCoords coords,
            int expectedX,
            int expectedZ,
            int expectedRegionX,
            int expectedRegionZ,
            int expectedRegionRelativeX,
            int expectedRegionRelativeZ
    ) {
        assertEquals(expectedX, coords.x);
        assertEquals(expectedZ, coords.z);

        assertEquals(expectedRegionX, coords.getRegionX());
        assertEquals(expectedRegionZ, coords.getRegionZ());

        assertEquals(expectedRegionRelativeX, coords.getRegionRelativeX());
        assertEquals(expectedRegionRelativeZ, coords.getRegionRelativeZ());

        ChunkCoords clone = new ChunkCoords(coords.x, coords.z);

        assertEquals(coords, clone);
        assertNotSame(clone, coords);

        assertEquals(coords.hashCode(), clone.hashCode());

        assertThrows(IllegalArgumentException.class, () -> coords.getRelative(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> coords.getRelative(16, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> coords.getRelative(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> coords.getRelative(0, 256, 0));
        assertThrows(IllegalArgumentException.class, () -> coords.getRelative(0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> coords.getRelative(0, 0, 16));

        IntPosition relative = coords.getRelative(1, 1, 1);

        assertEquals(coords.x * 16 + 1, relative.x);
        assertEquals(1, relative.y);
        assertEquals(coords.z * 16 + 1, relative.z);
    }

}
