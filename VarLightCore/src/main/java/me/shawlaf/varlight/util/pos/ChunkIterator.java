package me.shawlaf.varlight.util.pos;

import me.shawlaf.varlight.util.collections.PredictableSizeIterator;

public class ChunkIterator implements PredictableSizeIterator<ChunkCoords> {

    private final ChunkCoords start;
    private final ChunkCoords end;

    private final int xDirection;
    private final int zDirection;

    private int nextX;
    private int nextZ;

    private boolean next = true;

    public ChunkIterator(ChunkCoords start, ChunkCoords end) {
        this.start = start;
        this.end = end;

        this.xDirection = RegionIterator.binaryStep(end.x - start.x);
        this.zDirection = RegionIterator.binaryStep(end.z - start.z);

        this.nextX = start.x;
        this.nextZ = start.z;
    }

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

    public int getLengthX() {
        return Math.abs(end.x - start.x) + 1;
    }

    public int getLengthZ() {
        return Math.abs(end.z - start.z) + 1;
    }

    @Override
    public int getSize() {
        return getLengthX() * getLengthZ();
    }

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

    private boolean stepZ() {
        nextZ += zDirection;
        return zInRange(nextZ);
    }

    private boolean  stepX() {
        nextX += xDirection;
        return xInRange(nextX);
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
            if (stepX()) {
                return;
            }
        }

        next = false;
    }
}
