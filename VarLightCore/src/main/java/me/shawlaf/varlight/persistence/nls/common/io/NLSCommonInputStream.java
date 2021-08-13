package me.shawlaf.varlight.persistence.nls.common.io;

import me.shawlaf.varlight.persistence.nls.common.ChunkSectionNibbleArray;
import me.shawlaf.varlight.persistence.nls.common.NLSConstants;
import me.shawlaf.varlight.persistence.nls.common.NibbleArray;
import me.shawlaf.varlight.persistence.nls.common.exception.ExpectedMagicNumberException;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class NLSCommonInputStream extends DataInputStream {
    public NLSCommonInputStream(@NotNull InputStream in) {
        super(in);
    }

    public void verifyMagic() throws IOException {
        int expected = NLSConstants.NLS_MAGIC;
        int got = readInt();

        if (expected != got) {
            throw new ExpectedMagicNumberException(expected, got);
        }

    }

    public byte[] readNBytes(int amount) throws IOException {
        byte[] result = new byte[amount];
        readFillBytes(result);
        return result;
    }

    public void readFillBytes(byte[] buffer) throws IOException {
        int totalRead = 0;

        while (totalRead < buffer.length) {
            int read = read(buffer, totalRead, buffer.length - totalRead);

            if (read < 0) {
                throw new EOFException();
            }

            totalRead += read;
        }
    }

    public ChunkSectionNibbleArray readChunkSectionNibbleArray() throws IOException {
        return new ChunkSectionNibbleArray(readNBytes(ChunkSectionNibbleArray.SECTION_SIZE / 2));
    }

    public NibbleArray readNibbleArray(int size) throws IOException {
        if ((size & 1) != 0) {
            throw new IllegalArgumentException("Odd values not allowed");
        }

        return new NibbleArray(readNBytes(size / 2));
    }
}
