package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.FileUtil;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.Tuple;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static java.util.Objects.requireNonNull;
import static me.shawlaf.varlight.persistence.vldb.VLDBUtil.SIZEOF_INT32;
import static me.shawlaf.varlight.persistence.vldb.VLDBUtil.SIZEOF_MAGIC;

@Deprecated
public abstract class VLDBFile<L extends ICustomLightSource> {

    public static String FILE_NAME_FORMAT = "r.%d.%d.vldb2";
    public final File file;
    private final Object lock = new Object();
    private final int regionX, regionZ;
    private final boolean deflate;
    private byte[][] chunks = new byte[32 * 32][];
    private int nonEmptyChunks = 0;

    private boolean modified = false;

    public VLDBFile(@NotNull File file, int regionX, int regionZ, boolean deflate) throws IOException {
        this.file = requireNonNull(file);
        this.deflate = deflate;

        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    public VLDBFile(@NotNull File file, boolean deflate) throws IOException {
        this.file = requireNonNull(file);
        this.deflate = deflate;

        synchronized (lock) {
            try (InputStream stream = FileUtil.openStreamInflate(file)) {
                byte[] header = VLDBInputStream.readHeaderRaw(stream);

                try (VLDBInputStream in = new VLDBInputStream(new ByteArrayInputStream(header))) {
                    in.skip(SIZEOF_MAGIC);

                    this.regionX = in.readInt32();
                    this.regionZ = in.readInt32();
                }

                int amountChunks = ((int) header[3 * SIZEOF_INT32]) << 8 | ((int) header[3 * SIZEOF_INT32 + 1]);

                for (int i = 0; i < amountChunks; ++i) {
                    byte[] chunkData = VLDBInputStream.readChunkRaw(stream);

                    int x = chunkData[0];
                    int z = chunkData[1];

                    int chunkIndex = chunkIndex(x, z);

                    if (this.chunks[chunkIndex] != null) {
                        throw new IllegalStateException(String.format("Duplicate Chunk data for Chunk [%d, %d] in file %s", x, z, file.getAbsolutePath()));
                    }

                    this.chunks[chunkIndex(x, z)] = chunkData;
                    ++nonEmptyChunks;
                }
            }
        }
    }

    public static String getFileName(ICustomLightSource[] region) {
        final int rx = region[0].getPosition().getRegionX();
        final int rz = region[0].getPosition().getRegionZ();

        if (!allLightSourcesInRegion(rx, rz, region)) {
            throw new IllegalArgumentException("Not all light sources are in the same region!");
        }

        return String.format(FILE_NAME_FORMAT, rx, rz);
    }

    public static boolean allLightSourcesInRegion(int rx, int rz, ICustomLightSource[] lightSources) {
        for (ICustomLightSource iCustomLightSource : lightSources) {
            IntPosition pos = iCustomLightSource.getPosition();

            if (pos.getRegionX() != rx || pos.getRegionZ() != rz) {
                return false;
            }
        }

        return true;
    }

    public List<ChunkCoords> getChunksWithData() {
        synchronized (lock) {
            List<ChunkCoords> list = new ArrayList<>(nonEmptyChunks);

            for (int i = 0; i < chunks.length; ++i) {
                if (chunks[i] == null) {
                    continue;
                }

                int cx = i >> 5;
                int cz = i & 0x1F;

                list.add(new ChunkCoords(regionX * 32 + cx, regionZ * 32 + cz));
            }

            return list;
        }
    }

    @NotNull
    public L[] readChunk(int chunkX, int chunkZ) throws IOException {
        return readChunk(new ChunkCoords(chunkX, chunkZ));
    }

    @NotNull
    public L[] readChunk(@NotNull ChunkCoords chunkCoords) throws IOException {
        requireNonNull(chunkCoords);

        if (chunkCoords.getRegionX() != regionX || chunkCoords.getRegionZ() != regionZ) {
            throw new IllegalArgumentException(String.format("%s not in region %d %d", chunkCoords.toString(), regionX, regionZ));
        }

        int index = chunkIndex(chunkCoords);

        if (this.chunks[index] == null) {
            return createArray(0);
        }

//        if (!offsetTable.containsKey(chunkCoords)) {
//            return createArray(0);
//        }

        synchronized (lock) {
            try (VLDBInputStream in = in(chunks[index])) {
                return in.readChunk(regionX, regionZ, this::createArray, this::createInstance).item2;
            }
        }
    }

    @NotNull
    @Deprecated
    public List<L> readAll() throws IOException {
        synchronized (lock) {
            save();

            try (VLDBInputStream in = new VLDBInputStream(FileUtil.openStreamInflate(file))) {
                if (!in.readVLDBMagic()) {
                    throw new IllegalStateException("Could not identify VLDB magic");
                }

                return in.readAll(this::createArray, this::createInstance);
            }
        }
    }

    public boolean hasChunkData(int cx, int cz) {
        return hasChunkData(new ChunkCoords(cx, cz));
    }

    public boolean hasChunkData(ChunkCoords chunkCoords) {
        synchronized (lock) {
            return chunks[chunkIndex(chunkCoords)] != null;
        }
    }

    public void putChunk(@NotNull L[] chunk) throws IOException {
        requireNonNull(chunk);

        if (chunk.length == 0) {
            throw new IllegalArgumentException("Array may not be empty!");
        }

        final int cx = chunk[0].getPosition().getChunkX();
        final int cz = chunk[0].getPosition().getChunkZ();

        for (int i = 1; i < chunk.length; i++) {
            IntPosition pos = chunk[i].getPosition();

            if (pos.getChunkX() != cx || pos.getChunkZ() != cz) {
                throw new IllegalArgumentException("Not all Light sources are in the same chunk!");
            }
        }

        if ((cx >> 5) != regionX || (cz >> 5) != regionZ) {
            throw new IllegalArgumentException(String.format("Chunk %d %d not in region %d %d", cx, cz, regionX, regionZ));
        }

        synchronized (lock) {
            final ChunkCoords chunkCoords = new ChunkCoords(cx, cz); // TODO Write into the chunks[]

            final int index = chunkIndex(chunkCoords);

            Tuple<ByteArrayOutputStream, VLDBOutputStream> out = outToMemory();

            out.item2.writeChunk(cx, cz, chunk);
            out.item2.close();

            if (this.chunks[index] == null) {
                ++nonEmptyChunks;
            }

            this.chunks[index] = out.item1.toByteArray();
            this.modified = true;
        }
    }

    public void removeChunk(@NotNull ChunkCoords coords) throws IOException {
        requireNonNull(coords);

        if (coords.getRegionX() != regionX || coords.getRegionZ() != regionZ) {
            throw new IllegalArgumentException(String.format("Chunk %d %d not in region %d %d", coords.x, coords.z, regionX, regionZ));
        }

        synchronized (lock) {

            final int index = chunkIndex(coords);

            if (this.chunks[index] == null) {
                throw new IllegalStateException("Chunk not contained within this File!");
            }

            this.chunks[index] = null;
            --nonEmptyChunks;
            modified = true;
        }
    }

    public boolean isModified() {
        return modified;
    }

    public boolean save() throws IOException {
        synchronized (lock) {
            if (!modified || nonEmptyChunks == 0) {
                return false;
            }

            try (
                    OutputStream out = deflate ? new GZIPOutputStream(new FileOutputStream(file)) : new FileOutputStream(file)
            ) {
                final int headerSize = VLDBUtil.sizeofHeader(nonEmptyChunks);
                Tuple<ByteArrayOutputStream, VLDBOutputStream> headerBuffer = outToMemory(headerSize);

                headerBuffer.item2.writeInt32(VLDBInputStream.VLDB_MAGIC);
                headerBuffer.item2.writeInt32(regionX);
                headerBuffer.item2.writeInt32(regionZ);
                headerBuffer.item2.writeInt16(nonEmptyChunks);

                int offset = headerSize;
                int written = 0;

                LinkedList<Integer> chunkIndicesWithData = new LinkedList<>();

                for (int i = 0; i < chunks.length; i++) {
                    if (chunks[i] == null) {
                        continue;
                    }

                    chunkIndicesWithData.add(i);

                    int cx = i >> 5;
                    int cz = i & 0x1F;

                    headerBuffer.item2.writeInt16((cx << 8) | cz);
                    headerBuffer.item2.writeInt32(offset);

                    offset += chunks[i].length;

                    if (++written == nonEmptyChunks) {
                        break;
                    }
                }

                // Don't need to flush the headerBuffer because it's a BAOS.

                if (headerBuffer.item1.size() != headerSize) {
                    throw new IllegalStateException("Expected header size " + headerSize + " but got " + headerBuffer.item1.size());
                }

                byte[] header = headerBuffer.item1.toByteArray();

                out.write(header, 0, header.length);

                while (!chunkIndicesWithData.isEmpty()) {
                    int i = chunkIndicesWithData.removeFirst();

                    out.write(chunks[i], 0, chunks[i].length);
                }
            }

            modified = false;
            return true;
        }
    }

    @NotNull
    private VLDBInputStream in(byte[] data) {
        return new VLDBInputStream(new ByteArrayInputStream(data));
    }

    @NotNull
    private Tuple<ByteArrayOutputStream, VLDBOutputStream> outToMemory() {
        return outToMemory(32);
    }

    @NotNull
    private Tuple<ByteArrayOutputStream, VLDBOutputStream> outToMemory(int size) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);

        return new Tuple<>(baos, new VLDBOutputStream(baos));
    }

    @NotNull
    protected abstract L[] createArray(int size);

    @NotNull
    protected abstract L createInstance(IntPosition position, int lightLevel, boolean migrated, String material);

    public boolean delete() {
        synchronized (lock) {
            return file.delete();
        }
    }

    public void unload() {
        Arrays.fill(chunks, null);
        nonEmptyChunks = 0;
    }

    private int chunkIndex(ChunkCoords chunkCoords) {
        return chunkIndex(chunkCoords.getRegionRelativeX(), chunkCoords.getRegionRelativeZ());
    }

    private int chunkIndex(int cx, int cz) {
        return cz << 5 | cx;
    }
}
