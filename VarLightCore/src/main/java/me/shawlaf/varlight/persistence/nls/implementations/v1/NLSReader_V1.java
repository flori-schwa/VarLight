package me.shawlaf.varlight.persistence.nls.implementations.v1;

import lombok.Getter;
import me.shawlaf.varlight.persistence.nls.common.NLSHeader;
import me.shawlaf.varlight.persistence.nls.common.io.NLSCommonInputStream;

import java.io.IOException;
import java.io.InputStream;

public class NLSReader_V1 implements AutoCloseable {

    private final NLSCommonInputStream in;

    @Getter
    private final int regionX, regionZ;

    /**
     * Constructs a new NLSReader for Version 1, parses and verifies the Header from the InputStream
     * @param in The Stream to read from
     * @throws IOException if an {@link IOException} occurs
     */
    public NLSReader_V1(InputStream in) throws IOException {
        this(NLSHeader.readFromStream(in), in);
    }

    private NLSReader_V1(NLSHeader header, InputStream in) throws IOException {
        header.validRequired();

        if (header.getVersion() != 1) {
            throw new IllegalArgumentException("Expected NLS Version 1, got " + header.getVersion());
        }

        this.in = new NLSCommonInputStream(in);

        this.regionX = this.in.readInt();
        this.regionZ = this.in.readInt();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    public ChunkLightStorage_V1 readChunk() throws IOException {
        int encodedPosition = in.readShort();

        int chunkX = (encodedPosition & 0x1F);
        int chunkZ = (encodedPosition >>> 5) & 0x1F;

        ChunkLightStorage_V1 cls = new ChunkLightStorage_V1((32 * regionX) + chunkX, (32 * regionZ) + chunkZ);

        int mask = in.readShort();

        for (int y = 0; y < 16; ++y) {
            if ((mask & (1 << y)) == 0) {
                continue;
            }

            cls.lightData[y] = in.readChunkSectionNibbleArray();
        }

        return cls;
    }

}
