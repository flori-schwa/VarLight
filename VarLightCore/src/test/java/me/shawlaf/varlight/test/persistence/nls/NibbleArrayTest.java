package me.shawlaf.varlight.test.persistence.nls;

import me.shawlaf.varlight.persistence.nls.NibbleArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NibbleArrayTest {

    @Test
    public void testInvalidAmount() {
        assertThrows(IllegalArgumentException.class, () -> new NibbleArray(3));
    }

    @Test
    public void testInvalidWrite() {
        NibbleArray nibbleArray = new NibbleArray(2);

        assertThrows(IllegalArgumentException.class, () -> nibbleArray.set(0, -1));
        assertThrows(IllegalArgumentException.class, () -> nibbleArray.set(0, 16));
    }

    @Test
    public void testGet() {
        byte[] array = new byte[]{0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        NibbleArray nibbleArray = new NibbleArray(array);

        assertEquals(16, nibbleArray.length());

        for (int i = 0; i < 16; i++) {
            assertEquals(i, nibbleArray.get(i));
        }
    }

    @Test
    public void testSet() {
        NibbleArray nibbleArray = new NibbleArray(16);

        for (int i = 0; i < 16; i++) {
            nibbleArray.set(i, i);
        }

        for (int i = 0; i < 16; i++) {
            assertEquals(i, nibbleArray.get(i));
        }

        assertArrayEquals(new byte[]{0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF}, nibbleArray.toByteArray());

        for (int i = 0; i < 16; ++i) {
            nibbleArray.set(i, 0);
        }

        for (int i = 0; i < 16; i++) {
            assertEquals(0, nibbleArray.get(i));
        }

        assertArrayEquals(new byte[8], nibbleArray.toByteArray());

        nibbleArray.set(0, 4);
        nibbleArray.set(1, 6);

        assertEquals(4, nibbleArray.get(0));
        assertEquals(6, nibbleArray.get(1));

        nibbleArray.set(1, 3);

        assertEquals(4, nibbleArray.get(0));
        assertEquals(3, nibbleArray.get(1));

        nibbleArray.set(0, 2);

        assertEquals(2, nibbleArray.get(0));
        assertEquals(3, nibbleArray.get(1));
    }

}
