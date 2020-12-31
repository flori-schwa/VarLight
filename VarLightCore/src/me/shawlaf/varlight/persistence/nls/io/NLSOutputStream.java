package me.shawlaf.varlight.persistence.nls.io;

import me.shawlaf.varlight.persistence.nls.NLSUtil;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;

/*
    File Format:

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
public class NLSOutputStream implements Flushable, Closeable, AutoCloseable {

    private OutputStream out;

    public NLSOutputStream(OutputStream out) {
        this.out = out;
    }

    public void writeHeader(int regionX, int regionZ) throws IOException {
        writeInt32(NLSUtil.NLS_MAGIC);
        writeInt32(NLSUtil.CURRENT_VERSION);
        writeInt32(regionX);
        writeInt32(regionZ);
    }

    public void writeByte(int b) throws IOException {
        out.write(b);
    }

    public void writeInt16(int i16) throws IOException {
        byte[] buffer = new byte[2];

        buffer[0] = (byte) ((i16 >>> 8) & 0xFF);
        buffer[1] = (byte) (i16 & 0xFF);

        write(buffer);
    }

    public void writeInt32(int i32) throws IOException {
        byte[] buffer = new byte[4];

        buffer[0] = (byte) ((i32 >>> 24) & 0xFF);
        buffer[1] = (byte) ((i32 >>> 16) & 0xFF);
        buffer[2] = (byte) ((i32 >>> 8) & 0xFF);
        buffer[3] = (byte) (i32 & 0xFF);

        write(buffer);
    }

    public void write(byte[] buffer) throws IOException {
        out.write(buffer, 0, buffer.length);
    }

    public void write(byte[] buffer, int off, int len) throws IOException {
        out.write(buffer, off, len);
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }
}
