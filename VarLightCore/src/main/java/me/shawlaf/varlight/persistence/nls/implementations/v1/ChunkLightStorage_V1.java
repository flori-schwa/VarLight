package me.shawlaf.varlight.persistence.nls.implementations.v1;

import lombok.Getter;
import me.shawlaf.varlight.persistence.IChunkCustomLightAccess;
import me.shawlaf.varlight.persistence.nls.common.ChunkSectionNibbleArray;
import me.shawlaf.varlight.persistence.nls.common.NibbleArray;
import me.shawlaf.varlight.persistence.nls.common.exception.PositionOutOfBoundsException;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChunkLightStorage_V1 implements IChunkCustomLightAccess {

    private static final int SECTION_SIZE = 16 * 16 * 16;

    @Getter
    private final ChunkCoords chunkPosition;

    final ChunkSectionNibbleArray[] lightData = new ChunkSectionNibbleArray[16];

    public ChunkLightStorage_V1(ChunkCoords coords) {
        this.chunkPosition = coords;
    }

    public ChunkLightStorage_V1(int x, int z) {
        this(new ChunkCoords(x, z));
    }

    public int getCustomLuminance(IntPosition position) {
        int y = position.y >> 4;

        if (position.getChunkX() != chunkPosition.x || position.getChunkZ() != chunkPosition.z) {
            throw new PositionOutOfBoundsException(position);
        }

        if (y < 0 || y >= 16) {
            throw new PositionOutOfBoundsException(position);
        }

        NibbleArray section = lightData[y];

        if (section == null) {
            return 0;
        }

        return section.get(indexOf(position));
    }

    public void setCustomLuminance(IntPosition position, int value) {
        int y = position.y >> 4;

        if (position.getChunkX() != chunkPosition.x || position.getChunkZ() != chunkPosition.z) {
            throw new PositionOutOfBoundsException(position);
        }

        if (y < 0 || y >= 16) {
            throw new PositionOutOfBoundsException(position);
        }

        if (lightData[y] == null) {
            if (value > 0) {
                lightData[y] = new ChunkSectionNibbleArray();
            } else {
                return;
            }
        }

        lightData[y].set(indexOf(position), value);

        if (value == 0 && lightData[y].isEmpty()) {
            lightData[y] = null;
        }
    }

    @Override
    public @NotNull Iterator<IntPosition> iterateLightSources() {
        final int mask = getMask();

        return new Iterator<IntPosition>() {
            boolean hasNext;

            IntPosition next = null;
            int y = 0;
            int i = -1;

            {
                hasNext = findNextLightSource();
            }

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public IntPosition next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }

                IntPosition result = next;
                this.hasNext = findNextLightSource();

                return result;
            }

            private boolean findNextSection() {
                for (; y < 16; y++) {
                    if ((mask & y) != 0) {
                        return true;
                    }
                }

                return false;
            }

            private boolean findNextLightSource() {
                if (i < 0) {
                    if (!findNextSection()) {
                        return false;
                    }

                    i = 0;
                }

                NibbleArray arr = lightData[y];

                while (i < SECTION_SIZE) {
                    if (arr.get(i) > 0) {
                        next = fromIndex(y, i);
                        return true;
                    }
                }

                i = -1;
                return findNextLightSource();
            }
        };
    }

    public boolean isEmpty() {
        return !hasData();
    }

    @Override
    public boolean hasData() {
        for (int y = 0; y < 16; ++y) {
            if (getSection(y) == null) {
                continue;
            }

            return true;
        }

        return false;
    }

    @Override
    public void clear() {
        for (int y = 0; y < 16; y++) {
            this.lightData[y] = null;
        }
    }

    public int getMask() {
        int mask = 0;

        for (int y = 0; y < 16; ++y) {
            if (getSection(y) == null) {
                continue;
            }

            mask |= 1 << y;
        }

        return mask;
    }

    public void unload() {
        Arrays.fill(lightData, null);
    }

    public int encodePosition() {
        int encoded = 0;

        encoded |= chunkPosition.getRegionRelativeX();
        encoded |= (chunkPosition.getRegionRelativeZ() << 5);

        return encoded;
    }

    private ChunkSectionNibbleArray getSection(int y) {
        ChunkSectionNibbleArray section = lightData[y];

        if (section == null) {
            return null;
        }

        if (section.isEmpty()) {
            return (lightData[y] = null);
        }

        return section;
    }

    private int indexOf(IntPosition position) {
        return indexOf(position.getChunkRelativeX(), position.y & 0xF, position.getChunkRelativeZ());
    }

    private int indexOf(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    private IntPosition fromIndex(int sectionY, int index) {
        int x = chunkPosition.x * 16 + (index & 0xF);
        int y = sectionY * 16 + ((index >>> 8) & 0xF);
        int z = chunkPosition.z * 16 + ((index >>> 4) & 0xF);

        return new IntPosition(x, y, z);
    }
}
