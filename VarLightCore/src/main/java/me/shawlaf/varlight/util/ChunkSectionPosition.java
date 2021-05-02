package me.shawlaf.varlight.util;

import java.util.Objects;

public class ChunkSectionPosition {

    public static final ChunkSectionPosition ORIGIN = new ChunkSectionPosition(0, 0, 0);

    public final int x, y, z;

    public ChunkSectionPosition(ChunkCoords coords, int y) {
        this(coords.x, y, coords.z);
    }

    public ChunkSectionPosition(int x, int y, int z) {
        Preconditions.assertInRange("y", y, 0, 15);

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public ChunkSectionPosition toAbsolute(int regionX, int regionZ) {
        return new ChunkSectionPosition(regionX * 32 + x, y, regionZ * 32 + z);
    }

    public ChunkSectionPosition toRelative() {
        return new ChunkSectionPosition(getRegionRelativeX(), y, getRegionRelativeZ());
    }

    public int getRegionRelativeX() {
        return MathUtil.modulo(x, 32);
    }

    public int getRegionRelativeZ() {
        return MathUtil.modulo(z, 32);
    }

    public int encodeRegionRelative() {
        return (y << 10) | (getRegionRelativeZ() << 5) | getRegionRelativeX();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkSectionPosition that = (ChunkSectionPosition) o;
        return x == that.x &&
                y == that.y &&
                z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
