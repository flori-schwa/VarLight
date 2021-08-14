package me.shawlaf.varlight.persistence.nls.implementations.v1;

import me.shawlaf.varlight.persistence.nls.common.ChunkSectionNibbleArray;
import me.shawlaf.varlight.persistence.nls.common.io.NLSCommonOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/*
    V1 File Format:

    Header:
    [int32] MAGIC_VALUE 0x4E 0x41 0x4C 0x53
    [int32] VERSION
    [int32] REGION X
    [int32] REGION Z
    [Chunk[]]

    Chunk:
    [int16] POS IN REGION (ZZZZZ_XXXXX)
    [int16] Section Mask
    [NibbleArray(4096)[]] LIGHT DATA (2048 Bytes)
 */
public class NLSWriter_V1 implements AutoCloseable {

    private final NLSCommonOutputStream out;

    public NLSWriter_V1(OutputStream out) {
        this.out = new NLSCommonOutputStream(out);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public void writeHeader(int regionX, int regionZ) throws IOException {
        out.writeNLSMagic();

        // Version
        out.writeInt(1); // Not using CURRENT_VERSION Constant because this class is explicitly for version 1

        out.writeInt(regionX);
        out.writeInt(regionZ);
    }

    public void writeChunk(ChunkLightStorage_V1 cls) throws IOException {
        out.writeShort(cls.encodePosition());
        out.writeShort(cls.getMask());

        for (int y = 0; y < 16; y++) {
            ChunkSectionNibbleArray section = cls.lightData[y];

            if (section == null) {
                continue;
            }

            out.writeNibbleArray(section);
        }
    }
}
