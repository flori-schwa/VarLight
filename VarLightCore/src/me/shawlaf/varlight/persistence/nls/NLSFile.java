package me.shawlaf.varlight.persistence.nls;

import lombok.Getter;
import me.shawlaf.varlight.persistence.nls.io.NLSInputStream;
import me.shawlaf.varlight.persistence.nls.io.NLSOutputStream;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.FileUtil;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoords;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

public class NLSFile {

    private static final Logger LOGGER = Logger.getLogger(NLSFile.class.getSimpleName());

    public static String FILE_NAME_FORMAT = "r.%d.%d.nls";

    public final File file;

    private final Object lock = new Object();
    @Getter
    private final int regionX, regionZ;
    private final boolean deflate;

    private boolean modified;

    private ChunkLightStorage[] chunks = new ChunkLightStorage[32 * 32];
    @Getter
    private int nonEmptyChunks = 0;

    private NLSFile(@NotNull File file, int regionX, int regionZ, boolean deflate) {
        Objects.requireNonNull(file);

        if (file.exists()) {
            throw new IllegalArgumentException("File already exists!");
        }

        this.file = file;
        this.deflate = deflate;

        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    private NLSFile(@NotNull File file, boolean deflate) throws IOException {
        Objects.requireNonNull(file);

        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }

        this.file = file;
        this.deflate = deflate;

        synchronized (lock) {
            try (NLSInputStream in = openNLSFile(file)) {
                in.verifyNLSMagic();

                int version = in.readInt32();

                if (version != NLSUtil.CURRENT_VERSION) {
                    throw new IllegalStateException(String.format("Expected NLS Version %d, got %d", NLSUtil.CURRENT_VERSION, version));
                }

                this.regionX = in.readInt32();
                this.regionZ = in.readInt32();

                try {
                    while (true) {
                        int position = in.readInt16();

                        if (chunks[position] != null) {
                            throw new IllegalStateException(String.format("Duplicate Chunk Information for Chunk %s found in File %s", NLSUtil.fromEncoded(regionX, regionZ, position), file.getAbsolutePath()));
                        }

                        ChunkLightStorage cls = ChunkLightStorage.read(position, regionX, regionZ, in);

                        if (cls.isEmpty()) {
                            LOGGER.warning(String.format("Not loading Chunk %s because it is empty", NLSUtil.fromEncoded(regionX, regionZ, position).toShortString()));
                        } else {
                            chunks[position] = cls;
                            ++nonEmptyChunks;
                        }
                    }
                } catch (EOFException e) {
                    // Ignore
                }
            }
        }
    }

    public static NLSFile newFile(@NotNull File file, int regionX, int regionZ) {
        return new NLSFile(file, regionX, regionZ, true);
    }

    public static NLSFile newFile(@NotNull File file, int regionX, int regionZ, boolean deflate) {
        return new NLSFile(file, regionX, regionZ, deflate);
    }

    public static NLSFile existingFile(@NotNull File file) throws IOException {
        return new NLSFile(file, true);
    }

    public static NLSFile existingFile(@NotNull File file, boolean deflate) throws IOException {
        return new NLSFile(file, deflate);
    }

    public RegionCoords getRegionCoords() {
        return new RegionCoords(regionX, regionZ);
    }

    public int getCustomLuminance(IntPosition position) {
        synchronized (lock) {
            ChunkLightStorage chunk = chunks[chunkIndex(position.toChunkCoords())];

            if (chunk == null) {
                return 0;
            }

            return chunk.getCustomLuminance(position);
        }
    }

    public void setCustomLuminance(IntPosition position, int value) {
        ChunkCoords chunkCoords = position.toChunkCoords();
        int index = chunkIndex(chunkCoords);

        synchronized (lock) {
            ChunkLightStorage chunk = chunks[index];

            if (chunk == null) {
                if (value == 0) {
                    return;
                }

                chunk = new ChunkLightStorage(chunkCoords);

                chunk.setCustomLuminance(position, value);

                // The value set is not 0 -> The chunk is not empty, if the value is illegal, an exception will be thrown

                chunks[index] = chunk;
                ++nonEmptyChunks;

            } else {
                if (chunk.getCustomLuminance(position) == value) {
                    return;
                }

                chunk.setCustomLuminance(position, value);

                if (value == 0 && chunk.isEmpty()) { // If the last Light source was removed
                    chunks[index] = null;
                    --nonEmptyChunks;
                }
            }

            modified = true;
        }
    }

    public int getNonEmptyChunks() {
        int count = 0;

        synchronized (lock) {
            for (ChunkLightStorage chunk : chunks) {
                if (chunk == null) {
                    continue;
                }

                if (!chunk.isEmpty()) {
                    ++count;
                }
            }
        }

        return count;
    }

    public boolean hasChunkData(ChunkCoords chunkCoords) {
        int index = chunkIndex(chunkCoords);

        synchronized (lock) {
            return chunks[index] != null && !chunks[index].isEmpty();
        }
    }

    public void clearChunk(ChunkCoords chunkCoords) {
        int index = chunkIndex(chunkCoords);

        synchronized (lock) {
            if (chunks[index] == null || chunks[index].isEmpty()) {
                return;
            }

            chunks[index] = null;
            --nonEmptyChunks;
            modified = true;
        }
    }

    public int getMask(ChunkCoords chunkCoords) {
        synchronized (lock) {
            ChunkLightStorage cls = chunks[chunkIndex(chunkCoords)];

            if (cls == null) {
                return 0;
            }

            return cls.getMask();
        }
    }

    public boolean saveAndUnload() throws IOException {
        boolean saved = save();
        unload();

        return saved;
    }

    public boolean save() throws IOException {
        synchronized (lock) {
            if (!modified) {
                return false;
            }

            try (NLSOutputStream out = new NLSOutputStream(deflate ? new GZIPOutputStream(new FileOutputStream(file)) : new FileOutputStream(file))) {
                out.writeHeader(regionX, regionZ);
                ChunkLightStorage cls;

                for (int i = 0; i < chunks.length; ++i) {
                    cls = chunks[i];

                    if (cls == null) {
                        continue;
                    }

                    out.writeInt16(i);
                    cls.writeData(out);
                }
            }

            modified = false;
        }

        return true;
    }

    public List<ChunkCoords> getAffectedChunks() {
        List<ChunkCoords> list = new ArrayList<>(nonEmptyChunks);
        int found = 0;

        synchronized (lock) {
            for (int i = 0; i < chunks.length; ++i) {
                if (chunks[i] == null) {
                    continue;
                }

                list.add(chunks[i].getChunkCoords());

                if (++found == nonEmptyChunks) {
                    break;
                }
            }
        }

        return list;
    }

    @NotNull
    public List<IntPosition> getAllLightSources(ChunkCoords chunkCoords) {
        synchronized (lock) {
            ChunkLightStorage cls = chunks[chunkIndex(chunkCoords)];

            if (cls == null) {
                return new ArrayList<>();
            }

            return cls.getAllLightSources();
        }
    }

    @NotNull
    public List<IntPosition> getAllLightSources() {
        List<IntPosition> all = new ArrayList<>();

        synchronized (lock) {
            List<ChunkCoords> affected = getAffectedChunks();

            for (ChunkCoords chunkCoords : affected) {
                all.addAll(getAllLightSources(chunkCoords));
            }
        }

        return all;
    }

    public void unload() {
        if (modified) {
            LOGGER.warning("Unloading dirty NLS File " + file.getName());
            new Exception().printStackTrace();
        }

        synchronized (lock) {
            for (int i = 0; i < chunks.length; ++i) {
                if (chunks[i] == null) {
                    continue;
                }

                chunks[i].unload();
                chunks[i] = null;
            }
        }
    }

    private NLSInputStream openNLSFile(File file) throws IOException {
        return new NLSInputStream(FileUtil.openStreamInflate(file));
    }

    private int chunkIndex(ChunkCoords chunkCoords) {
        return chunkIndex(chunkCoords.getRegionRelativeX(), chunkCoords.getRegionRelativeZ());
    }

    private int chunkIndex(int cx, int cz) {
        return cz << 5 | cx;
    }
}
