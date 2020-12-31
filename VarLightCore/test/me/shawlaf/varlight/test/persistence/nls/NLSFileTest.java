package me.shawlaf.varlight.test.persistence.nls;

import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class NLSFileTest {

    private byte[] buildTestData(int version, int regionX, int regionZ) {
        byte[] testData = new byte[4 * 4 + 2 * 2 + 2048];

        int i = 0;

        testData[i++] = 0x4E;
        testData[i++] = 0x41;
        testData[i++] = 0x4C;
        testData[i++] = 0x53;

        testData[i++] = (byte) ((version >>> 24) & 0xFF);
        testData[i++] = (byte) ((version >>> 16) & 0xFF);
        testData[i++] = (byte) ((version >>> 8) & 0xFF);
        testData[i++] = (byte) ((version) & 0xFF);

        testData[i++] = (byte) ((regionX >>> 24) & 0xFF);
        testData[i++] = (byte) ((regionX >>> 16) & 0xFF);
        testData[i++] = (byte) ((regionX >>> 8) & 0xFF);
        testData[i++] = (byte) ((regionX) & 0xFF);

        testData[i++] = (byte) ((regionZ >>> 24) & 0xFF);
        testData[i++] = (byte) ((regionZ >>> 16) & 0xFF);
        testData[i++] = (byte) ((regionZ >>> 8) & 0xFF);
        testData[i++] = (byte) ((regionZ) & 0xFF);

        testData[i++] = 0;
        testData[i++] = 0;

        testData[i++] = 0;
        testData[i++] = 1;

        testData[i++] = 0x01;
        testData[i++] = 0x23;
        testData[i++] = 0x45;
        testData[i++] = 0x67;
        testData[i++] = (byte) 0x89;
        testData[i++] = (byte) 0xAB;
        testData[i++] = (byte) 0xCD;
        testData[i++] = (byte) 0xEF;

        Arrays.fill(testData, i, testData.length - 1, (byte) 0);

        return testData;
    }

    private void writeGzipped(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fos)) {
                gzipOutputStream.write(data, 0, data.length);
            }
        }
    }

    @Test
    public void testRead(@TempDir File tempDir) throws IOException {
        byte[] testData = buildTestData(1, 0, 0);

        File file = new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, 0, 0));

        writeGzipped(file, testData);

        NLSFile nlsFile = NLSFile.existingFile(file);

        for (int x = 0; x < 16; ++x) {
            assertEquals(x, nlsFile.getCustomLuminance(new IntPosition(x, 0, 0)));
        }
    }

    @Test
    public void testWrite(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, 0, 0));
        NLSFile nlsFile = NLSFile.newFile(file, 0, 0);

        assertFalse(nlsFile.save());

        for (int x = 0; x < 16; ++x) {
            nlsFile.setCustomLuminance(new IntPosition(x, 0, 0), x);
        }

        assertEquals(1, nlsFile.getNonEmptyChunks());
        assertTrue(nlsFile.saveAndUnload());

        nlsFile = NLSFile.existingFile(file);

        assertEquals(1, nlsFile.getNonEmptyChunks());

        for (int x = 0; x < 16; ++x) {
            assertEquals(x, nlsFile.getCustomLuminance(new IntPosition(x, 0, 0)));
        }
    }

    @Test
    public void testFragmentedWrite(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, 0, 0));
        NLSFile nlsFile = NLSFile.newFile(file, 0, 0);

        nlsFile.setCustomLuminance(new IntPosition(0, 0, 0), 1);
        nlsFile.setCustomLuminance(new IntPosition(0, 32, 0), 2);
        nlsFile.setCustomLuminance(new IntPosition(0, 33, 0), 3);
        nlsFile.setCustomLuminance(new IntPosition(0, 65, 0), 4);

        assertEquals(1, nlsFile.getNonEmptyChunks());
        assertEquals(0b10101, nlsFile.getMask(ChunkCoords.ORIGIN));

        assertTrue(nlsFile.saveAndUnload());

        nlsFile = NLSFile.existingFile(file);

        assertEquals(1, nlsFile.getNonEmptyChunks());
        assertEquals(0b10101, nlsFile.getMask(ChunkCoords.ORIGIN));

        assertEquals(1, nlsFile.getCustomLuminance(new IntPosition(0, 0, 0)));
        assertEquals(2, nlsFile.getCustomLuminance(new IntPosition(0, 32, 0)));
        assertEquals(3, nlsFile.getCustomLuminance(new IntPosition(0, 33, 0)));
        assertEquals(4, nlsFile.getCustomLuminance(new IntPosition(0, 65, 0)));
    }

//    @Test
//    public void stressTest(@TempDir File tempDir) throws IOException {
//        File file = new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, 0, 0));
//        NLSFile nlsFile = NLSFile.newFile(file, 0, 0);
//
//        long start = System.currentTimeMillis();
//        long lastSplit = start;
//        long now;
//
//        System.out.println("[0ms] Writing");
//
//        int i = 1;
//
//        for (int y = 0; y < 256; ++y) {
//            for (int z = 0; z < 32 * 16; ++z) {
//                for (int x = 0; x < 32 * 16; ++x) {
//                    nlsFile.setCustomLuminance(new IntPosition(x, y, z), i);
//
//                    if (++i == 16) {
//                        i = 1;
//                    }
//                }
//            }
//        }
//
//        now = System.currentTimeMillis();
//        System.out.println("[" + (now - start) + "ms] Saving (Writing took: " + (now - lastSplit) + "ms)");
//        lastSplit = System.currentTimeMillis();
//
//        assertEquals(32 * 32, nlsFile.getNonEmptyChunks());
//
//        assertTrue(nlsFile.saveAndUnload());
//
//        now = System.currentTimeMillis();
//        System.out.println("[" + (now - start) + "ms] Reading (Saving took: " + (now - lastSplit) + "ms)");
//        lastSplit = now;
//
//        nlsFile = NLSFile.existingFile(file);
//
//        assertEquals(32 * 32, nlsFile.getNonEmptyChunks());
//
//        i = 1;
//
//        now = System.currentTimeMillis();
//        System.out.println("[" + (now - start) + "ms] Verifying (Reading took: " + (now - lastSplit) + "ms)");
//
//        for (int y = 0; y < 256; ++y) {
//            for (int z = 0; z < 32 * 16; ++z) {
//                for (int x = 0; x < 32 * 16; ++x) {
//                    assertEquals(i, nlsFile.getCustomLuminance(new IntPosition(x, y, z)));
//
//                    if (++i == 16) {
//                        i = 1;
//                    }
//                }
//            }
//        }
//    }

    @Test
    public void testGetAffectedChunks(@TempDir File tempDir) throws IOException {

        int regionX = 0;
        int regionZ = 0;

        File file = new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, regionX, regionZ));
        NLSFile nlsFile = NLSFile.newFile(file, regionX, regionZ);

        IntPosition regionOrigin = new IntPosition(32 * 16 * regionX, 0, 32 * 16 * regionZ);

        for (int cz = 0; cz < 32; ++cz) {
            for (int cx = 0; cx < 32; ++cx) {
                if ((cx & 1) != 0 || (cz & 1) != 0) {
                    continue;
                }

                nlsFile.setCustomLuminance(regionOrigin.getRelative(16 * cx, 0, 16 * cz), 15);
            }
        }

        assertEquals(256, nlsFile.getNonEmptyChunks());
        List<ChunkCoords> affected = nlsFile.getAffectedChunks();

        for (int cz = 0; cz < 32; ++cz) {
            for (int cx = 0; cx < 32; ++cx) {
                boolean bothEven = (cx & 1) == 0 && (cz & 1) == 0;
                ChunkCoords chunkCoords = new ChunkCoords(32 * regionX + cx, 32 * regionZ + cz);

                if (bothEven) {
                    assertTrue(affected.contains(chunkCoords));
                } else {
                    assertFalse(affected.contains(chunkCoords));
                }
            }
        }

        assertTrue(nlsFile.saveAndUnload());

        nlsFile = NLSFile.existingFile(file);

        assertEquals(256, nlsFile.getNonEmptyChunks());
        affected = nlsFile.getAffectedChunks();

        for (int cz = 0; cz < 32; ++cz) {
            for (int cx = 0; cx < 32; ++cx) {
                boolean bothEven = (cx & 1) == 0 && (cz & 1) == 0;
                ChunkCoords chunkCoords = new ChunkCoords(32 * regionX + cx, 32 * regionZ + cz);

                if (bothEven) {
                    assertTrue(affected.contains(chunkCoords));
                } else {
                    assertFalse(affected.contains(chunkCoords));
                }
            }
        }
    }

    @Test
    public void testRemoveLastLightSource(@TempDir File tempDir) {
        int regionX = 0;
        int regionZ = 0;

        File file = new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, regionX, regionZ));
        NLSFile nlsFile = NLSFile.newFile(file, regionX, regionZ);

        ChunkCoords regionOriginChunk = new ChunkCoords(32 * regionX, 32 * regionZ);
        IntPosition regionOrigin = regionOriginChunk.getRelative(0, 0, 0);

        nlsFile.setCustomLuminance(regionOrigin, 15);

        assertEquals(1, nlsFile.getNonEmptyChunks());
        assertTrue(nlsFile.getAffectedChunks().contains(regionOriginChunk));
        assertEquals(1, nlsFile.getMask(regionOriginChunk));

        nlsFile.setCustomLuminance(regionOrigin.getRelative(0, 16, 0), 15);

        assertEquals(1, nlsFile.getNonEmptyChunks());
        assertTrue(nlsFile.getAffectedChunks().contains(regionOriginChunk));
        assertEquals(0b11, nlsFile.getMask(regionOriginChunk));

        nlsFile.setCustomLuminance(regionOrigin, 0);

        assertEquals(1, nlsFile.getNonEmptyChunks());
        assertTrue(nlsFile.getAffectedChunks().contains(regionOriginChunk));
        assertEquals(0b10, nlsFile.getMask(regionOriginChunk));

        nlsFile.setCustomLuminance(regionOrigin.getRelative(0, 16, 0), 0);

        assertEquals(0, nlsFile.getNonEmptyChunks());
        assertFalse(nlsFile.getAffectedChunks().contains(regionOriginChunk));
        assertEquals(0, nlsFile.getMask(regionOriginChunk));
    }

    @Test
    public void testNextIndexChunk(@TempDir File tempDir) {
        int regionX = 0;
        int regionZ = 0;

        File file = new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, regionX, regionZ));
        NLSFile nlsFile = NLSFile.newFile(file, regionX, regionZ);

        ChunkCoords regionOriginChunk = new ChunkCoords(32 * regionX, 32 * regionZ);
        IntPosition regionOrigin = regionOriginChunk.getRelative(0, 0, 0);

        nlsFile.setCustomLuminance(regionOrigin, 15);
        nlsFile.setCustomLuminance(regionOrigin.getRelative(16, 0, 0), 15);

        List<ChunkCoords> affected = nlsFile.getAffectedChunks();

        assertEquals(2, nlsFile.getNonEmptyChunks());
        assertEquals(nlsFile.getNonEmptyChunks(), affected.size());

        assertTrue(affected.contains(regionOriginChunk));
        assertTrue(affected.contains(regionOriginChunk.getRelativeChunk(1, 0)));
    }

    @Test
    public void testWrongVersion(@TempDir File tempDir) throws IOException {
        byte[] testData = buildTestData(4, 0, 0);

        File file = new File(tempDir, "r.0.0.nls");

        writeGzipped(file, testData);

        assertThrows(IllegalStateException.class, () -> NLSFile.existingFile(file, true));
    }

    @Test
    public void testFileNotExist(@TempDir File tempDir) {
        File file = new File(tempDir, "r.0.0.nls");

        assertThrows(IllegalArgumentException.class, () -> NLSFile.existingFile(file, true));
    }

}
