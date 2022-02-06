package me.shawlaf.varlight.persistence.nls;

import lombok.Getter;
import me.shawlaf.varlight.persistence.IChunkCustomLightAccess;
import me.shawlaf.varlight.persistence.IRegionCustomLightAccess;
import me.shawlaf.varlight.persistence.nls.common.NLSConstants;
import me.shawlaf.varlight.persistence.nls.common.NLSHeader;
import me.shawlaf.varlight.persistence.nls.common.migrate.NLSMigrators;
import me.shawlaf.varlight.persistence.nls.implementations.v1.ChunkLightStorage_V1;
import me.shawlaf.varlight.persistence.nls.implementations.v1.NLSReader_V1;
import me.shawlaf.varlight.persistence.nls.implementations.v1.NLSWriter_V1;
import me.shawlaf.varlight.util.io.FileUtil;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import me.shawlaf.varlight.util.pos.RegionCoords;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

public class NLSFile implements IRegionCustomLightAccess {

    private static final Logger LOGGER = Logger.getLogger(NLSFile.class.getSimpleName());

    public static String FILE_NAME_FORMAT = "r.%d.%d.nls";

    public final File file;

    @Getter
    private final int regionX, regionZ;
    private final boolean deflate;

    private boolean dirty;
    private int nonEmptyChunks = 0;

    private final ChunkLightStorage_V1[] chunks = new ChunkLightStorage_V1[32 * 32];

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

        boolean needsMigration = false;

        try (InputStream iStream = FileUtil.openStreamInflate(file)) {
            NLSHeader header = NLSHeader.readFromStream(iStream);

            if (header.getVersion() < NLSConstants.CURRENT_VERSION) {
                needsMigration = true;
            } else if (header.getVersion() > NLSConstants.CURRENT_VERSION) {
                throw new IllegalStateException(String.format("Cannot downgrade from future NLS Version %d, current version: %d", header.getVersion(), NLSConstants.CURRENT_VERSION));
            }
        }

        if (needsMigration) {
            NLSMigrators.migrateFileToVersion(NLSConstants.CURRENT_VERSION, file);
        }

        try (InputStream iStream = FileUtil.openStreamInflate(file)) {
            try (NLSReader_V1 reader = new NLSReader_V1(iStream)) {
                // Header already parsed and verified by constructor

                this.regionX = reader.getRegionX();
                this.regionZ = reader.getRegionZ();

                try {
                    while (true) {
                        ChunkLightStorage_V1 cls = reader.readChunk();

                        int index = cls.encodePosition();

                        if (chunks[index] != null) {
                            throw new IllegalStateException(String.format("Duplicate Chunk Information for Chunk %s found in File %s", cls.getChunkPosition().toShortString(), file.getAbsolutePath()));
                        }

                        if (cls.isEmpty()) {
                            LOGGER.warning(String.format("Not loading Chunk %s because it is empty", cls.getChunkPosition().toShortString()));
                        } else {
                            chunks[index] = cls;
                            ++nonEmptyChunks;
                        }
                    }
                } catch (EOFException ignored) {

                }
            }
        }
    }

    public static File getFile(File parent, RegionCoords regionCoords) {
        return getFile(parent, regionCoords.x, regionCoords.z);
    }

    public static File getFile(File parent, int regionX, int regionZ) {
        return new File(parent, String.format(FILE_NAME_FORMAT, regionX, regionZ));
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

    @Override
    public int getCustomLuminance(IntPosition position) {
        synchronized (this) {
            IChunkCustomLightAccess chunk = chunks[chunkIndex(position.toChunkCoords())];

            if (chunk == null) {
                return 0;
            }

            return chunk.getCustomLuminance(position);
        }
    }

    @Override
    public int setCustomLuminance(IntPosition position, int value) {
        ChunkCoords chunkCoords = position.toChunkCoords();
        int index = chunkIndex(chunkCoords);
        int ret = 0;

        synchronized (this) {
            ChunkLightStorage_V1 chunk = chunks[index];

            if (chunk == null) {
                // No Data present

                if (value == 0) {
                    return 0;
                }

                chunk = new ChunkLightStorage_V1(chunkCoords);

                chunk.setCustomLuminance(position, value);

                // The value set is not 0 -> The chunk is not empty, if the value is illegal, an exception will be thrown

                chunks[index] = chunk;
                ++nonEmptyChunks;
                return 0;
            } else {
                ret = chunk.getCustomLuminance(position);

                if (ret == value) {
                    return ret;
                }

                chunk.setCustomLuminance(position, value);

                if (value == 0 && !chunk.hasData()) { // If the last Light source was removed
                    chunks[index] = null;
                    --nonEmptyChunks;
                }
            }

            dirty = true;
        }

        return ret;
    }

    @Override
    public int getNonEmptyChunks() {
        int count = 0;

        synchronized (this) {
            for (IChunkCustomLightAccess chunk : chunks) {
                if (chunk == null) {
                    continue;
                }

                if (chunk.hasData()) {
                    ++count;
                }
            }
        }

        return count;
    }

    @Override
    public void clearChunk(ChunkCoords chunkCoords) {
        int index = chunkIndex(chunkCoords);

        synchronized (this) {
            if (chunks[index] == null || !chunks[index].hasData()) {
                return;
            }

            chunks[index] = null;
            --nonEmptyChunks;
            dirty = true;
        }
    }

    @Override
    public int getMask(ChunkCoords chunkCoords) {
        synchronized (this) {
            IChunkCustomLightAccess cls = chunks[chunkIndex(chunkCoords)];

            if (cls == null) {
                return 0;
            }

            return cls.getMask();
        }
    }

    @Override
    public IChunkCustomLightAccess getChunk(ChunkCoords chunkCoords) {
        return chunks[chunkIndex(chunkCoords)];
    }

    public boolean saveAndUnload() throws IOException {
        boolean saved = save();
        unload();

        return saved;
    }

    public boolean save() throws IOException {
        synchronized (this) {
            if (!dirty) {
                return false;
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                OutputStream oStream = deflate ? new GZIPOutputStream(fos) : fos;

                try (NLSWriter_V1 out = new NLSWriter_V1(oStream)) {
                    out.writeHeader(regionX, regionZ);

                    for (ChunkLightStorage_V1 cls : chunks) {
                        if (cls == null) {
                            continue;
                        }

                        out.writeChunk(cls);
                    }
                }
            }

            dirty = false;
        }

        return true;
    }

    @Override
    public @NotNull List<ChunkCoords> getAffectedChunks() {
        List<ChunkCoords> list = new ArrayList<>(nonEmptyChunks);
        int found = 0;

        synchronized (this) {
            for (ChunkLightStorage_V1 chunk : chunks) {
                if (chunk == null) {
                    continue;
                }

                list.add(chunk.getChunkPosition());

                if (++found == nonEmptyChunks) {
                    break;
                }
            }
        }

        return list;
    }

    @Override
    public @NotNull Iterator<IntPosition> iterateAllLightSources() {
        Queue<Iterator<IntPosition>> chunkIterators = new LinkedList<>();

        for (ChunkCoords chunk : getAffectedChunks()) {
            chunkIterators.add(iterateLightSources(chunk));
        }

        return new Iterator<IntPosition>() {
            Iterator<IntPosition> current = chunkIterators.poll();

            @Override
            public boolean hasNext() {
                return current != null && current.hasNext();
            }

            @Override
            public IntPosition next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                IntPosition result = current.next();

                if (!current.hasNext()) {
                    current = chunkIterators.poll();
                }

                return result;
            }
        };
    }

    public void unload() {
        if (dirty) {
            LOGGER.warning("Unloading dirty NLS File " + file.getName());
            new Exception().printStackTrace();
        }

        synchronized (this) {
            for (int i = 0; i < chunks.length; ++i) {
                if (chunks[i] == null) {
                    continue;
                }

                chunks[i].unload();
                chunks[i] = null;
            }
        }
    }

    private int chunkIndex(ChunkCoords chunkCoords) {
        return chunkIndex(chunkCoords.getRegionRelativeX(), chunkCoords.getRegionRelativeZ());
    }

    private int chunkIndex(int cx, int cz) {
        return cz << 5 | cx;
    }
}
