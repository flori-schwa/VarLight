package me.shawlaf.varlight.persistence.nls.common;

import java.util.Arrays;

public class ChunkSectionNibbleArray extends NibbleArray {
    public static final int SECTION_SIZE = 16 * 16 * 16;

    private static final byte[] EMPTY_CHUNK_SECTION = new byte[SECTION_SIZE / 2];

    public ChunkSectionNibbleArray() {
        super(SECTION_SIZE);
    }

    public ChunkSectionNibbleArray(byte[] array) {
        super(array);

        if (array.length != (SECTION_SIZE / 2)) {
            throw new IllegalArgumentException(String.format("Array length must be %d, got: %d", SECTION_SIZE / 2, array.length));
        }
    }

    public boolean isEmpty() {
        return Arrays.equals(this.array, EMPTY_CHUNK_SECTION);
    }
}
