package me.shawlaf.varlight.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class IntPosition implements Comparable<IntPosition> {

    public static final IntPosition ORIGIN = new IntPosition(0, 0, 0);

    public final int x, y, z;

    public IntPosition(long val) {
        this((int) (val >> 38), (int) (val & 0xFFF), (int) (val << 26 >> 38));
    }

    public IntPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getChunkRelativeX() {
        return x & 0xF;
    }

    public int getChunkRelativeZ() {
        return z & 0xF;
    }

    public int getChunkX() {
        return x >> 4;
    }

    public int getChunkZ() {
        return z >> 4;
    }

    public int getRegionX() {
        return getChunkX() >> 5;
    }

    public int getRegionZ() {
        return getChunkZ() >> 5;
    }

    public int xDistanceTo(IntPosition other) {
        return Math.abs(other.x - this.x);
    }

    public int yDistanceTo(IntPosition other) {
        return Math.abs(other.y - this.y);
    }

    public int zDistanceTo(IntPosition other) {
        return Math.abs(other.z - this.z);
    }

    public boolean outOfBounds() {
        return y < 0 || y > 255;
    }

    public ChunkSectionPosition getChunkSection() {
        return new ChunkSectionPosition(getChunkX(), y >> 4, getChunkZ());
    }

    public int manhattanDistance(IntPosition other) {
        Objects.requireNonNull(other);

        int total = 0;

        total += Math.abs(x - other.x);
        total += Math.abs(y - other.y);
        total += Math.abs(z - other.z);

        return total;
    }

    public IntPosition getRelative(int dx, int dy, int dz) {
        return new IntPosition(x + dx, y + dy, z + dz);
    }

    public long encode() {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) z & 0x3FFFFFF) << 12) | ((long) y & 0xFFF);
    }

    public ChunkCoords toChunkCoords() {
        return new ChunkCoords(getChunkX(), getChunkZ());
    }

    public RegionCoords toRegionCoords() {
        return new RegionCoords(getRegionX(), getRegionZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntPosition that = (IntPosition) o;
        return x == that.x &&
                y == that.y &&
                z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "IntPosition{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public String toShortString() {
        return String.format("[%d, %d, %d]", x, y, z);
    }

    @Override
    public int compareTo(@NotNull IntPosition o) {
        return Integer.compare(this.manhattanDistance(ORIGIN), o.manhattanDistance(ORIGIN));
    }
}
