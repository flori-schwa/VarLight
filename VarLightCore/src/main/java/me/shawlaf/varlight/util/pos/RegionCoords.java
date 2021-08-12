package me.shawlaf.varlight.util.pos;


public class RegionCoords {

    public final int x, z;

    public RegionCoords(IntPosition intPosition) {
        this(intPosition.getChunkX() >> 5, intPosition.getChunkZ() >> 5);
    }

    public RegionCoords(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ChunkCoords getRegionStartChunk() {
        return new ChunkCoords(x * 32, z * 32);
    }

    public ChunkCoords getRegionEndChunk() {
        return new ChunkCoords((x * 32) + 31, (z * 32) + 31);
    }

    public IntPosition getRegionStart() {
        return getRegionStartChunk().getChunkStart();
    }

    public IntPosition getRegionEnd() {
        return getRegionEndChunk().getChunkEnd();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionCoords that = (RegionCoords) o;
        return x == that.x &&
                z == that.z;
    }

    @Override
    public int hashCode() {
        int result = 89 * 113 + x;
        return 89 * result + z;
    }

    @Override
    public String toString() {
        return String.format("%d %d", x, z);
    }
}
