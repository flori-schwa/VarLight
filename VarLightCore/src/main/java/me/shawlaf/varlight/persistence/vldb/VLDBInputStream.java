package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.FileUtil;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.Tuple;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntFunction;
import java.util.zip.GZIPInputStream;

import static me.shawlaf.varlight.persistence.vldb.VLDBUtil.SIZEOF_OFFSET_TABLE_ENTRY;

@Deprecated
public class VLDBInputStream implements Closeable {

    public static int VLDB_MAGIC = 0x56_4C_44_42;

    protected final DataInputStream baseStream;

    public VLDBInputStream(DataInputStream baseStream) {
        this.baseStream = baseStream;
    }

    public VLDBInputStream(InputStream inputStream) {
        this(new DataInputStream(inputStream));
    }

    public static boolean verifyVLDB(File file) throws IOException {
        VLDBInputStream in;
        boolean isVLDB;

        try (FileInputStream fis = new FileInputStream(file)) {
            if (FileUtil.isDeflated(file)) {
                in = new VLDBInputStream(new GZIPInputStream(fis));
            } else {
                in = new VLDBInputStream(fis);
            }

            isVLDB = in.readVLDBMagic();
        }


        in.close();

        return isVLDB;
    }

    public static byte[] readHeaderRaw(InputStream inputStream) throws IOException {
        VLDBInputStream in = new VLDBInputStream(inputStream);

        if (!in.readVLDBMagic()) {
            throw new IllegalStateException("VLDB Magic not found");
        }

        int regionX = in.readInt32();
        int regionZ = in.readInt32();

        int amountChunks = in.readInt16();

        byte[] offsetTable = in.readBytes(VLDBUtil.sizeofOffsetTable(amountChunks));

        ByteArrayOutputStream baos = new ByteArrayOutputStream(VLDBUtil.sizeofHeader(amountChunks));

        try (VLDBOutputStream out = new VLDBOutputStream(baos)) {
            out.writeInt32(VLDB_MAGIC);
            out.writeInt32(regionX);
            out.writeInt32(regionZ);

            out.writeInt16(amountChunks);

            out.write(offsetTable, 0, offsetTable.length);
        }

        return baos.toByteArray();
    }

    public static byte[] readChunkRaw(InputStream inputStream) throws IOException {
        VLDBInputStream in = new VLDBInputStream(inputStream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (VLDBOutputStream out = new VLDBOutputStream(baos)) {
            out.writeInt16(in.readInt16()); // Chunk coords in region

            int lightSources = in.readUInt24();
            out.writeUInt24(lightSources);

            for (int i = 0; i < lightSources; i++) {
                out.write(in.readBytes(3), 0, 3); // Position in chunk and light data

                int asciiLen = in.readInt16();

                out.writeInt16(asciiLen);
                out.write(in.readBytes(asciiLen), 0, asciiLen);
            }
        }

        return baos.toByteArray();
    }

    @Override
    public void close() throws IOException {
        this.baseStream.close();
    }

    public boolean readVLDBMagic() throws IOException {
        return readInt32() == VLDB_MAGIC;
    }

    public <L extends ICustomLightSource> List<L> readAll(IntFunction<L[]> arrayCreator, ToLightSource<L> toLightSource) throws IOException {

        final int regionX = readInt32();
        final int regionZ = readInt32();

        List<L> lightSources = new ArrayList<>();

        final int amountChunks = readInt16();

        skip(amountChunks * SIZEOF_OFFSET_TABLE_ENTRY); // Skip header

        for (int i = 0; i < amountChunks; i++) {
            lightSources.addAll(Arrays.asList(readChunk(regionX, regionZ, arrayCreator, toLightSource).item2));
        }

        return lightSources;
    }

    public <L extends ICustomLightSource> Tuple<ChunkCoords, L[]> readChunk(int regionX, int regionZ, IntFunction<L[]> arrayCreator, ToLightSource<L> toLightSource) throws IOException {
        ChunkCoords chunkCoords = readEncodedChunkCoords(regionX, regionZ);

        int amountLightSources = readUInt24();

        L[] lightSources = arrayCreator.apply(amountLightSources);

        for (int j = 0; j < amountLightSources; j++) {
            int coords;

            try {
                coords = readInt16();
            } catch (EOFException e) {
                throw e;
            }

            int data = readByte();
            String material = readASCII();

            IntPosition position = chunkCoords.getRelative(
                    ((coords & 0xF000) >>> 12),
                    (coords & 0x0FF0) >>> 4,
                    (coords & 0xF)
            );

            int lightLevel = (data & 0xF0) >>> 4;
            boolean migrated = (data & 0x0F) != 0;

            lightSources[j] = toLightSource.toLightSource(position, lightLevel, migrated, material);
        }

        return new Tuple<>(chunkCoords, lightSources);
    }

    public Map<ChunkCoords, Integer> readHeader(int regionX, int regionZ) throws IOException {
        final int amountChunks = readInt16();

        final Map<ChunkCoords, Integer> header = new HashMap<>(amountChunks);

        for (int i = 0; i < amountChunks; i++) {
            ChunkCoords chunkCoords = readEncodedChunkCoords(regionX, regionZ);
            int offset = readInt32();

            header.put(chunkCoords, offset);
        }

        return header;
    }

    public ChunkCoords readEncodedChunkCoords(int regionX, int regionZ) throws IOException {
        int encodedCoords = readInt16();

        int cx = ((encodedCoords & 0xFF00) >>> 8) + regionX * 32;
        int cz = (encodedCoords & 0xFF) + regionZ * 32;

        return new ChunkCoords(cx, cz);
    }

    public void skip(int n) throws IOException {
        baseStream.skipBytes(n);
    }

    public int readByte() throws IOException {
        return baseStream.readUnsignedByte();
    }

    public int readInt16() throws IOException {
        return baseStream.readUnsignedShort();
    }

    public int readUInt24() throws IOException {
        return readByte() << 16 | readByte() << 8 | readByte();
    }

    public int readInt32() throws IOException {
        return baseStream.readInt();
    }

    public byte[] readBytes(int amount) throws IOException {
        byte[] ret = new byte[amount];
        int read = 0, written = 0;

        while (written < amount) {
            read = baseStream.read(ret, written, amount - written);
            written += read;

            if (read == -1) {
                throw new EOFException();
            }
        }

        return ret;
    }

    public String readASCII() throws IOException {
        return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(readBytes(readInt16()))).toString();
    }

    @FunctionalInterface
    public interface ToLightSource<L extends ICustomLightSource> {
        L toLightSource(IntPosition position, int lightLevel, boolean migrated, String material);
    }
}
