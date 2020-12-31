package me.shawlaf.varlight.persistence.nls;

import me.shawlaf.varlight.util.Preconditions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class NibbleArray {

    protected final byte[] array;

    public NibbleArray(int size) {
        if ((size & 1) != 0) {
            throw new IllegalArgumentException("Odd values not allowed");
        }

        this.array = new byte[size >> 1];
    }

    public NibbleArray(byte[] array) {
        this.array = Arrays.copyOf(array, array.length);
    }

    public int length() {
        return array.length * 2;
    }

    public int get(int index) {
        int b = array[index / 2];

        if ((index & 1) == 0) {
            return (b >>> 4) & 0xF;
        }

        return b & 0xF;
    }

    public void set(int index, int value) {
        Preconditions.assertInRange("value", value, 0x0, 0xF);

        int b = array[index >> 1];

        if ((index & 1) == 0) {
            b = (value << 4) | (b & 0x0F);
        } else {
            b = (b & 0xF0) | value;
        }

        array[index >> 1] = (byte) b;
    }

    @Deprecated
    public boolean isAllZeroes() {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(array, array.length);
    }

    public void write(OutputStream out) throws IOException {
        out.write(array, 0, array.length);
    }


}
