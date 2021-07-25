package me.shawlaf.varlight.persistence.nls.common.implementations.v1;

import me.shawlaf.varlight.persistence.nls.common.ChunkSectionNibbleArray;
import me.shawlaf.varlight.persistence.nls.common.io.NLSCommonOutputStream;

import java.io.IOException;
import java.io.OutputStream;

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
