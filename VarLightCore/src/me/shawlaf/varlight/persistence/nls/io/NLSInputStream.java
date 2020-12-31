package me.shawlaf.varlight.persistence.nls.io;

import me.shawlaf.varlight.persistence.nls.NLSUtil;
import me.shawlaf.varlight.persistence.nls.NibbleArray;

import java.io.*;

public class NLSInputStream implements Closeable {

    private DataInputStream in;

    public NLSInputStream(InputStream in) {
        this(new DataInputStream(in));
    }

    public NLSInputStream(DataInputStream in) {
        this.in = in;
    }

    public void verifyNLSMagic() throws IOException {
        if (readInt32() != NLSUtil.NLS_MAGIC) {
            throw new IllegalStateException("Could not Identify NLS Header");
        }
    }

    public NibbleArray readNibbleArray(int size) throws IOException {
        if ((size & 1) != 0) {
            throw new IllegalArgumentException("Odd values not allowed");
        }

        return new NibbleArray(readBytes(size / 2));
    }

    public int readByte() throws IOException {
        return in.readUnsignedByte();
    }

    public int readInt16() throws IOException {
        return in.readUnsignedShort();
    }

    public int readInt32() throws IOException {
        return in.readInt();
    }

    public void skip(int n) throws IOException {
        in.skipBytes(n);
    }

    public byte[] readBytes(int amount) throws IOException {
        byte[] ret = new byte[amount];
        int read = 0, written = 0;

        while (written < amount) {
            read = in.read(ret, written, amount - written);
            written += read;

            if (read == -1) {
                throw new EOFException();
            }
        }

        return ret;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
