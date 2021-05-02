package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static java.lang.Integer.parseInt;

@UtilityClass
public class FileUtil {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\..+$");

    public static String getExtension(File file) {
        String path = file.getAbsolutePath();

        return path.substring(path.lastIndexOf('.'));
    }

    public static boolean deleteRecursively(File file) {
        if (!file.isDirectory()) {
            return file.delete();
        }

        File[] files = file.listFiles();

        if (files == null) {
            return file.delete();
        }

        for (File f : files) {
            if (!deleteRecursively(f)) {
                return false;
            }
        }

        return file.delete();
    }

    public static InputStream openStreamInflate(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);

        if (isDeflated(file)) {
            return new GZIPInputStream(fis);
        } else {
            return fis;
        }
    }

    public static byte[] readFileFullyInflate(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (FileInputStream fis = new FileInputStream(file)) {
            InputStream in;

            if (isDeflated(file)) {
                in = new GZIPInputStream(fis);
            } else {
                in = fis;
            }

            byte[] buffer = new byte[1024];
            int read = 0;

            while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                baos.write(buffer, 0, read);
            }

            in.close();
        }

        return baos.toByteArray();
    }

    public static boolean isDeflated(File file) throws IOException {
        boolean deflated = false;

        try (FileInputStream fis = new FileInputStream(file)) {
            DataInputStream dataInputStream = new DataInputStream(fis);

            int lsb = dataInputStream.readUnsignedByte();
            int msb = dataInputStream.readUnsignedByte();

            int read = (msb << 8) | lsb;

            if (read == GZIPInputStream.GZIP_MAGIC) {

                read = dataInputStream.readByte();

                if (read == 0x08) {
                    deflated = true;
                }
            }

            dataInputStream.close();
        }

        return deflated;
    }

    @Nullable
    public static RegionCoords parseRegionCoordsFromFileName(String fileName) {
        Matcher matcher = FILENAME_PATTERN.matcher(fileName);

        if (!matcher.matches()) {
            return null;
        }

        return new RegionCoords(parseInt(matcher.group(1)), parseInt(matcher.group(2)));
    }

}
