package me.shawlaf.varlight.spigot.util;

import me.shawlaf.varlight.spigot.util.collections.IteratorUtils;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionIterator implements Iterator<IntPosition> {

    @NotNull
    public final IntPosition start, end;
    private final int xDirection, yDirection, zDirection;

    private boolean next;
    private int nextX, nextY, nextZ;

    public static Iterator<ChunkCoords> squareChunkArea(ChunkCoords center, int radius) {
        ChunkCoords a = center.getRelativeChunk(-radius, -radius);
        ChunkCoords b = center.getRelativeChunk(radius, radius);

        return new RegionIterator(a.getChunkStart(), b.getChunkStart()).iterateChunks();
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

    public int getLengthY()  {
        return Math.abs(end.y - start.y) + 1;
    }

    public int getLengthZ() {
        return Math.abs(end.z - start.z) + 1;
    }

    public int getSize() {
        return getLengthX() * getLengthY() * getLengthZ();
    }

    // region Chunk Iterator

    public Set<ChunkCoords> getAllContainingChunks() {
        return IteratorUtils.collectFromIterator(iterateChunks(), Collectors.toSet());
    }

    public Iterator<ChunkCoords> iterateChunks() {
        final int startChunkX = start.getChunkX();
        final int startChunkZ = start.getChunkZ();

        final int endChunkX = end.getChunkX();
        final int endChunkZ = end.getChunkZ();

        final int xDirection = binaryStep(endChunkX - startChunkX);
        final int zDirection = binaryStep(endChunkZ - startChunkZ);

        return new Iterator<ChunkCoords>() {

            int nextX = startChunkX;
            int nextZ = startChunkZ;

            boolean next = true;

            @Override
            public boolean hasNext() {
                return next;
            }

            @Override
            public ChunkCoords next() {
                if (!next) {
                    throw new IndexOutOfBoundsException("There are no more elements left to iterate over");
                }

                ChunkCoords nextCoords = new ChunkCoords(nextX, nextZ);
                step();

                return nextCoords;
            }

            private void step() {
                if (zDirection != 0) {
                    nextZ += zDirection;

                    boolean zOutOfRange;

                    if (zDirection > 0) {
                        zOutOfRange = nextZ < startChunkZ || nextZ > endChunkZ;
                    } else {
                        zOutOfRange = nextZ > startChunkZ || nextX < endChunkZ;
                    }

                    if (zOutOfRange) {
                        nextZ = startChunkZ;
                    } else {
                        return;
                    }
                }

                if (xDirection != 0) {
                    nextX += xDirection;

                    boolean xOutOfRange;

                    if (xDirection > 0) {
                        xOutOfRange = nextX < startChunkX || nextX > endChunkX;
                    } else {
                        xOutOfRange = nextX > startChunkX || nextX < endChunkX;
                    }

                    if (xOutOfRange) {
                        next = false;
                    }
                }
            }
        };
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

    private void step() {
        if (zDirection != 0) {
            nextZ += zDirection;

            boolean zOutOfRange;

            if (zDirection > 0) {
                zOutOfRange = nextZ < start.z || nextZ > end.z;
            } else {
                zOutOfRange = nextZ > start.z || nextZ < end.z;
            }

            if (zOutOfRange) {
                nextZ = start.z;
            } else {
                return;
            }
        }

        if (xDirection != 0) {
            nextX += xDirection;

            boolean xOutOfRange;

            if (xDirection > 0) {
                xOutOfRange = nextX < start.x || nextX > end.x;
            } else {
                xOutOfRange = nextX > start.x || nextX < end.x;
            }

            if (xOutOfRange) {
                nextX = start.x;
            } else {
                return;
            }
        }

        if (yDirection != 0) {
            nextY += yDirection;

            boolean yOutOfRange;

            if (yDirection > 0) {
                yOutOfRange = nextY < start.y || nextY > end.y;
            } else {
                yOutOfRange = nextY > start.y || nextY < end.y;
            }

            if (yOutOfRange) {
                next = false;
            }
        }
    }

    private static int binaryStep(int x) {
        /*
         * x < 0 -> -1
         * x = 0 ->  0
         * x > 0 ->  1
         */
        return Integer.compare(x, 0);
    }

    // endregion
}
