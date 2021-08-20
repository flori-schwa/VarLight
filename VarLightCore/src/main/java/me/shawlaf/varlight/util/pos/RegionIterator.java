package me.shawlaf.varlight.util.pos;

import me.shawlaf.varlight.util.collections.IteratorUtils;
import me.shawlaf.varlight.util.collections.PredictableSizeIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionIterator implements PredictableSizeIterator<IntPosition> {

    @NotNull
    public final IntPosition start, end;
    private final int xDirection, yDirection, zDirection;

    private boolean next;
    private int nextX, nextY, nextZ;

    public static ChunkIterator squareChunkArea(ChunkCoords center, int radius) {
        ChunkCoords a = center.getRelativeChunk(-radius, -radius);
        ChunkCoords b = center.getRelativeChunk(radius, radius);

        return new ChunkIterator(a, b);
    }

    public RegionIterator(@NotNull IntPosition start, @NotNull IntPosition end) {
        this.start = Objects.requireNonNull(start, "Start Position must not be null");
        this.end = Objects.requireNonNull(end, "End Position must not be null");

        this.xDirection = binaryStep(end.x - start.x);
        this.yDirection = binaryStep(end.y - start.y);
        this.zDirection = binaryStep(end.z - start.z);

        this.nextX = start.x;
        this.nextY = start.y;
        this.nextZ = start.z;

        this.next = true;
    }

    public int getLengthX() {
        return Math.abs(end.x - start.x) + 1;
    }

    public int getLengthY() {
        return Math.abs(end.y - start.y) + 1;
    }

    public int getLengthZ() {
        return Math.abs(end.z - start.z) + 1;
    }

    @Override
    public int getSize() {
        return getLengthX() * getLengthY() * getLengthZ();
    }

    // region Chunk Iterator

    public Set<ChunkCoords> getAllContainingChunks() {
        return IteratorUtils.collectFromIterator(iterateChunks(), Collectors.toSet());
    }

    public ChunkIterator iterateChunks() {
        return new ChunkIterator(start.toChunkCoords(), end.toChunkCoords());
    }

    // endregion

    @Override
    public boolean hasNext() {
        return next;
    }

    @Override
    @NotNull
    public IntPosition next() {
        if (!next) {
            throw new IndexOutOfBoundsException("There are no more elements left to iterate over");
        }

        IntPosition nextPos = new IntPosition(nextX, nextY, nextZ);
        step();

        return nextPos;
    }

    // region Util

    private boolean zInRange(int z) {
        if (start.z < end.z) {
            return z >= start.z && z <= end.z;
        }

        return z >= end.z && z <= start.z;
    }

    private boolean xInRange(int x) {
        if (start.x < end.x) {
            return x >= start.x && x <= end.x;
        }

        return x >= end.x && x <= start.x;
    }

    private boolean yInRange(int y) {
        if (start.y < end.y) {
            return y >= start.y && y <= end.y;
        }

        return y >= end.y && y <= start.y;
    }

    private boolean stepZ() {
        nextZ += zDirection;
        return zInRange(nextZ);
    }

    private boolean stepX() {
        nextX += xDirection;
        return xInRange(nextX);
    }

    private boolean stepY() {
        nextY += yDirection;
        return yInRange(nextY);
    }

    private void step() {

        if (zDirection != 0) {
            if (!stepZ()) {
                nextZ = start.z;
            } else {
                return;
            }
        }

        if (xDirection != 0) {
            if (!stepX()) {
                nextX = start.x;
            } else {
                return;
            }
        }

        if (yDirection != 0) {
            if (stepY()) {
                return;
            }
        }

        next = false;
    }

    static int binaryStep(int x) {
        /*
         * x < 0 -> -1
         * x = 0 ->  0
         * x > 0 ->  1
         */
        return Integer.compare(x, 0);
    }

    // endregion
}
