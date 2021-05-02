package me.shawlaf.varlight.persistence.nls;

import lombok.Getter;
import me.shawlaf.varlight.persistence.nls.exception.PositionOutOfBoundsException;
import me.shawlaf.varlight.persistence.nls.io.NLSInputStream;
import me.shawlaf.varlight.persistence.nls.io.NLSOutputStream;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkLightStorage {

    private static final int SECTION_SIZE = 16 * 16 * 16;
    private static final byte[] EMPTY_CHUNK_SECTION = new byte[SECTION_SIZE / 2];

    @Getter
    private final int chunkX;
    @Getter
    private final int chunkZ;
    private final NibbleArray[] lightData = new NibbleArray[16];

    public ChunkLightStorage(ChunkCoords coords) {
        this(coords.x, coords.z);
    }

    public ChunkLightStorage(int x, int z) {
        this.chunkX = x;
        this.chunkZ = z;
    }

    protected static ChunkLightStorage read(int encodedPosition, int regionX, int regionZ, NLSInputStream in) throws IOException {
        ChunkLightStorage cls = new ChunkLightStorage(32 * regionX + (encodedPosition & 0x1F), 32 * regionZ + ((encodedPosition >>> 5) & 0x1F));
        int mask = in.readInt16();

        for (int y = 0; y < 16; ++y) {
            if ((mask & (1 << y)) == 0) {
                continue;
            }

            cls.lightData[y] = in.readNibbleArray(SECTION_SIZE);
        }

        return cls;
    }

    public int getCustomLuminance(IntPosition position) {
        int y = position.y >> 4;

        if (position.getChunkX() != chunkX || position.getChunkZ() != chunkZ) {
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

        if (position.getChunkX() != chunkX || position.getChunkZ() != chunkZ) {
            throw new PositionOutOfBoundsException(position);
        }

        if (y < 0 || y >= 16) {
            throw new PositionOutOfBoundsException(position);
        }

        if (lightData[y] == null) {
            if (value > 0) {
                lightData[y] = new NibbleArray(16 * 16 * 16);
            } else {
                return;
            }
        }

        lightData[y].set(indexOf(position), value);

        if (value == 0 && Arrays.equals(lightData[y].array, EMPTY_CHUNK_SECTION)) {
            lightData[y] = null;
        }
    }

    public List<IntPosition> getAllLightSources() {
        List<IntPosition> lightSources = new ArrayList<>();

        int mask = getMask();

        for (int y = 0; y < 16; ++y) {
            NibbleArray arr = lightData[y];

            if ((mask & (1 << y)) == 0) {
                continue;
            }

            for (int i = 0; i < SECTION_SIZE; ++i) {
                if (arr.get(i) > 0) {
                    lightSources.add(fromIndex(y, i));
                }
            }
        }

        return lightSources;
    }

    public boolean isEmpty() {
        return getMask() == 0;
    }

    public int getMask() {
        int mask = 0;

        for (int y = 0; y < 16; ++y) {
            if (lightData[y] == null || Arrays.equals(lightData[y].array, EMPTY_CHUNK_SECTION)) {
                continue;
            }

            mask |= 1 << y;
        }

        return mask;
    }

    public void unload() {
        for (int i = 0; i < lightData.length; ++i) {
            lightData[i] = null;
        }
    }

    public ChunkCoords getChunkCoords() {
        return new ChunkCoords(chunkX, chunkZ);
    }

    protected void writeData(NLSOutputStream out) throws IOException {
        int mask = getMask();

        out.writeInt16(mask);

        for (int y = 0; y < 16; ++y) {
            if ((mask & (1 << y)) == 0) {
                continue;
            }

            NibbleArray nibbleArray = lightData[y];

            out.write(nibbleArray.array, 0, nibbleArray.array.length);
        }
    }

    private int indexOf(IntPosition position) {
        return indexOf(position.getChunkRelativeX(), position.y & 0xF, position.getChunkRelativeZ());
    }

    private int indexOf(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    private IntPosition fromIndex(int sectionY, int index) {
        int x = chunkX * 16 + (index & 0xF);
        int y = sectionY * 16 + ((index >>> 8) & 0xF);
        int z = chunkZ * 16 + ((index >>> 4) & 0xF);

        return new IntPosition(x, y, z);
    }
}
