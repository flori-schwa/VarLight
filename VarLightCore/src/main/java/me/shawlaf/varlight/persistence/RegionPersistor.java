package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.persistence.vldb.VLDBFile;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Deprecated
public abstract class RegionPersistor<L extends ICustomLightSource> {

    private static final int REGION_SIZE = 32;
    private static final int CHUNK_SIZE = 16 * 16 * 256;

    public final int regionX, regionZ;

    public final VLDBFile<L> file;

    private final Object chunkLock = new Object();

    private final L[][] chunkCache = createMultiArr(REGION_SIZE * REGION_SIZE);
    private final int[] chunkSizes = new int[REGION_SIZE * REGION_SIZE];
    private final List<ChunkCoords> dirtyChunks = new ArrayList<>(REGION_SIZE * REGION_SIZE);

    public RegionPersistor(@NotNull File vldbRoot, int regionX, int regionZ, boolean deflated) throws IOException {
        Objects.requireNonNull(vldbRoot);

        if (!vldbRoot.exists()) {
            if (!vldbRoot.mkdir()) {
                throw new LightPersistFailedException("Could not create directory \"" + vldbRoot.getAbsolutePath() + "\"");
            }
        }

        if (!vldbRoot.isDirectory()) {
            throw new IllegalArgumentException(String.format("\"%s\" is not a directory!", vldbRoot.getAbsolutePath()));
        }

        this.regionX = regionX;
        this.regionZ = regionZ;

        File vldbFile = new File(vldbRoot, String.format(VLDBFile.FILE_NAME_FORMAT, regionX, regionZ));

        if (!vldbFile.exists()) {
            this.file = new VLDBFile<L>(vldbFile, regionX, regionZ, deflated) {
                @NotNull
                @Override
                protected L[] createArray(int size) {
                    return RegionPersistor.this.createArray(size);
                }

                @NotNull
                @Override
                protected L createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return RegionPersistor.this.createInstance(position, lightLevel, migrated, material);
                }
            };
        } else {
            this.file = new VLDBFile<L>(vldbFile, deflated) {
                @NotNull
                @Override
                protected L[] createArray(int size) {
                    return RegionPersistor.this.createArray(size);
                }

                @NotNull
                @Override
                protected L createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return RegionPersistor.this.createInstance(position, lightLevel, migrated, material);
                }
            };
        }
    }

    /**
     * <p>Marks the Chunk containing the specified {@link IntPosition} as dirty.</p>
     *
     * @param position The Position, where changes to Light sources have occured.
     */
    public void markDirty(IntPosition position) {
        markDirty(position.toChunkCoords());
    }

    /**
     * <p>Marks the Chunk with the specified {@link ChunkCoords} as dirty.</p>
     *
     * @param chunkCoords The Position of the Chunk, where changes to Light sources have occured.
     */
    public void markDirty(ChunkCoords chunkCoords) {
        assertInRegion(chunkCoords);

        synchronized (chunkLock) {
            dirtyChunks.add(chunkCoords);
        }
    }

    /**
     * <p>Loads The Light sources of the specified Chunk from the raw data into the cache.</p>
     *
     * @param chunkCoords The Coordinates of the Chunk, whose Custom Light data should be loaded.
     * @throws IOException When an {@link IOException} occurs while reading the data.
     */
    public void loadChunk(@NotNull ChunkCoords chunkCoords) throws IOException {
        Objects.requireNonNull(chunkCoords);
        assertInRegion(chunkCoords);

        final int chunkIndex = chunkIndex(chunkCoords);

        synchronized (chunkLock) {
            L[] lightSources;

            synchronized (file) {
                lightSources = file.readChunk(chunkCoords);
            }

            L[] fullChunk = createArray(CHUNK_SIZE);

            int count = 0;

            for (L ls : lightSources) {
                int index = indexOf(ls.getPosition());

                if (fullChunk[index] != null) {
                    throw new IllegalStateException("Duplicate Lightsource at Position " + ls.getPosition().toShortString());
                }

                fullChunk[index] = ls;
                ++count;
            }

            chunkSizes[chunkIndex] = count;
            chunkCache[chunkIndex] = fullChunk;
        }
    }

    /**
     * <p>Checks if the Custom Light data for the Chunk with the specified {@link ChunkCoords} is currently loaded into the cache.</p>
     *
     * @param chunkCoords The {@link ChunkCoords} to check
     * @return true, if the Light data for the Chunk is loaded into the cache.
     */
    public boolean isChunkLoaded(@NotNull ChunkCoords chunkCoords) {
        Objects.requireNonNull(chunkCoords);
        assertInRegion(chunkCoords);

        synchronized (chunkLock) {
            return chunkCache[chunkIndex(chunkCoords)] != null;
        }
    }

    /**
     * <p>Unloads the Custom Light data for the Chunk with the specified {@link ChunkCoords} from the cache.</p>
     * <p>If the Chunk was marked as dirty in {@link RegionPersistor#markDirty(ChunkCoords)} or {@link RegionPersistor#markDirty(IntPosition)} the Chunk will be flushed.</p>
     *
     * @param chunkCoords The {@link ChunkCoords} of the Chunk, whose Light data should be unloaded from the cache.
     * @throws IOException If an {@link IOException} occurs while flushing the Chunk.
     */
    public void unloadChunk(@NotNull ChunkCoords chunkCoords) throws IOException {
        Objects.requireNonNull(chunkCoords);
        assertInRegion(chunkCoords);

        final int chunkIndex = chunkIndex(chunkCoords);

        synchronized (chunkLock) {
            L[] toUnload = chunkCache[chunkIndex];

            if (toUnload == null) { // There was no mapping for the chunk
                return;
            }

            if (dirtyChunks.contains(chunkCoords)) {
                flushChunk(chunkCoords, getNonNullFromChunk(chunkCoords));
            }

            chunkSizes[chunkIndex] = 0;
            chunkCache[chunkIndex] = null;
        }
    }

    /**
     * <p>Returns the current Cache for the Chunk with the specified {@link ChunkCoords} as a List of Light sources.</p>
     *
     * @param chunkCoords The {@link ChunkCoords} of the Chunk, whose cache is being queried.
     * @return A {@link List} of type {@code T}, the Light source data, currently in cache.
     */
    @NotNull
    public List<L> getCache(@NotNull ChunkCoords chunkCoords) {
        Objects.requireNonNull(chunkCoords);
        assertInRegion(chunkCoords);

        List<L> chunk;

        synchronized (chunkLock) {
            L[] chunkArray = chunkCache[chunkIndex(chunkCoords)];

            if (chunkArray == null) {
                chunk = new ArrayList<>();
            } else {
                chunk = new ArrayList<>(getNonNullFromChunk(chunkCoords));
            }
        }

        return Collections.unmodifiableList(chunk);
    }

    /**
     * <p>Looks up Custom Light source Data at the specified {@link IntPosition}, if the Chunk containing the Position is not currently loaded in the cache,
     * The Chunk will be loaded using {@link RegionPersistor#loadChunk(ChunkCoords)}</p>
     *
     * @param position The Position to look up.
     * @return An instance of {@code L} or {@code null} if there is now Light source at the given Position
     * @throws IOException If an {@link IOException} occurs during {@link RegionPersistor#loadChunk(ChunkCoords)}
     */
    @Nullable
    public L getLightSource(@NotNull IntPosition position) throws IOException {
        Objects.requireNonNull(position);
        assertInRegion(position);

        final ChunkCoords chunkCoords = position.toChunkCoords();
        final int chunkIndex = chunkIndex(chunkCoords);

        synchronized (chunkLock) {
            if (chunkCache[chunkIndex] == null) {
                loadChunk(chunkCoords);
            }

            return chunkCache[chunkIndex][indexOf(position)];
        }
    }

    /**
     * <p>Inserts the Lightsource at Position {@link ICustomLightSource#getPosition()} if no Light Source exists at that position yet.</p>
     * <p>Modifies the Lightsource at the Position, if a Light Source already exists at the Position and {@link ICustomLightSource#getCustomLuminance()} is {@code > 0}</p>
     * <p>Deletes the Lightsource at the Position, if {@link ICustomLightSource#getCustomLuminance()} is {@code 0}.</p>
     * <p>
     * <br />
     *
     * <p>If The Chunk containing {@link ICustomLightSource#getPosition()} is not yet loaded into the cache, {@link RegionPersistor#loadChunk(ChunkCoords)} will be called.</p>
     *
     * @param lightSource The Light source to insert
     * @throws IOException If an {@link IOException} occurs during {@link RegionPersistor#loadChunk(ChunkCoords)}
     */
    public void put(@NotNull L lightSource) throws IOException {
        Objects.requireNonNull(lightSource);
        assertInRegion(lightSource.getPosition());

        final ChunkCoords chunkCoords = lightSource.getPosition().toChunkCoords();
        final int chunkIndex = chunkIndex(chunkCoords);

        synchronized (chunkLock) {
            if (chunkCache[chunkIndex] == null) {
                loadChunk(chunkCoords);
            }

            putInternal(lightSource);
        }
    }

    @Deprecated
    public void removeLightSource(@NotNull IntPosition position) throws IOException {
        Objects.requireNonNull(position);
        assertInRegion(position);

        final ChunkCoords chunkCoords = position.toChunkCoords();
        final int chunkIndex = chunkIndex(chunkCoords);
        final int index = indexOf(position);

        synchronized (chunkLock) {
            if (chunkCache[chunkIndex] == null) {
                loadChunk(chunkCoords);
            }

            L[] chunkArray = chunkCache[chunkIndex];

            if (chunkArray[index] != null) {
                chunkArray[index] = null;
                --chunkSizes[chunkIndex];

                markDirty(chunkCoords);
            }
        }
    }

    /**
     * <p>Flushes all dirty chunks.</p>
     *
     * @throws IOException If an {@link IOException} occurs during flushing.
     */
    public void flushAll() throws IOException {
        synchronized (chunkLock) {
            synchronized (file) {
                for (ChunkCoords key : dirtyChunks.toArray(new ChunkCoords[dirtyChunks.size()])) {
                    flushChunk(key);
                }
            }
        }
    }

    /**
     * @return A {@link List} of {@link ChunkCoords} that contain Custom Light data inside the Region.
     */
    public List<ChunkCoords> getAffectedChunks() {
        synchronized (file) {
            return file.getChunksWithData();
        }
    }

    /**
     * @return A {@link List} containing all Light Sources inside this Region, all modified chunks will first be flushed.
     * @throws IOException If an {@link IOException} occurs while flushing or reading.
     */
    public List<L> loadAll() throws IOException {
        synchronized (file) {
            synchronized (chunkLock) {
                int cx, cz;

                for (int z = 0; z < REGION_SIZE; ++z) {
                    for (int x = 0; x < REGION_SIZE; ++x) {
                        int chunkIndex = chunkIndex(cx = regionX + x, cz = regionZ + z);
                        ChunkCoords chunkCoords = new ChunkCoords(cx, cz);

                        if (chunkCache[chunkIndex] != null && dirtyChunks.contains(chunkCoords)) {
                            flushChunk(chunkCoords, getNonNullFromChunk(chunkCoords));
                        }
                    }
                }

                return file.readAll();
            }
        }
    }

    /**
     * Saves the currently flushed data on the disk. Modified, but not yet flushed changes will not be saved.
     *
     * @return true, if data was written to the disk (changes have been made)
     * @throws IOException If an {@link IOException} occurs.
     */
    public boolean save() throws IOException {
        synchronized (file) {
            return file.save();
        }
    }

    private void assertInRegion(IntPosition position) {
        if (position.getRegionX() != regionX || position.getRegionZ() != regionZ) {
            throw new IllegalArgumentException(String.format("Position %s is not in region [%d, %d]", position.toShortString(), regionX, regionZ));
        }
    }

    private void assertInRegion(ChunkCoords chunkCoords) {
        if (chunkCoords.getRegionX() != regionX || chunkCoords.getRegionZ() != regionZ) {
            throw new IllegalArgumentException(String.format("Chunk %s is not in region [%d, %d]", chunkCoords.toShortString(), regionX, regionZ));
        }
    }

    private void flushChunk(ChunkCoords chunkCoords, Collection<L> lightData) throws IOException {
        final int chunkIndex = chunkIndex(chunkCoords);

        synchronized (chunkLock) {
            if (!dirtyChunks.contains(chunkCoords)) {
                return;
            }

            synchronized (file) {
                if (lightData.size() == 0) {
                    if (file.hasChunkData(chunkCoords)) {
                        file.removeChunk(chunkCoords);
                    }

                    chunkCache[chunkIndex] = null;
                    chunkSizes[chunkIndex] = 0;
                }

                file.putChunk(lightData.toArray(createArray(lightData.size())));
            }

            dirtyChunks.remove(chunkCoords);
        }
    }

    private void flushChunk(ChunkCoords chunkCoords) throws IOException {
        synchronized (chunkLock) {
            flushChunk(chunkCoords, getNonNullFromChunk(chunkCoords));
        }
    }

    private void putInternal(L lightSource) {
        Objects.requireNonNull(lightSource);

        final ChunkCoords chunkCoords = lightSource.getPosition().toChunkCoords();
        final int chunkIndex = chunkIndex(chunkCoords);
        final int index = indexOf(lightSource.getPosition());

        synchronized (chunkLock) {
            L[] chunkArray = chunkCache[chunkIndex];

            if (chunkArray == null) {
                throw new IllegalArgumentException("No Data present for chunk");
            }

            L removed = chunkArray[index];
            chunkArray[index] = null;

            if (lightSource.getCustomLuminance() > 0) { // New or modified
                chunkArray[index] = lightSource;

                if (removed == null) {
                    ++chunkSizes[chunkIndex]; // One new light source added
                }

                // When a light source was modified, aka removed != null, then the amount of Light sources stays the same

                markDirty(chunkCoords);
            } else { // Removed, or no-op
                if (removed != null) {
                    markDirty(chunkCoords);
                    --chunkSizes[chunkIndex]; // One Light source was removed
                }
            }
        }
    }

    private Collection<L> getNonNullFromChunk(ChunkCoords chunkCoords) {
        final int chunkIndex = chunkIndex(chunkCoords);

        synchronized (chunkLock) {
            int chunkSize = chunkSizes[chunkIndex];

            List<L> list = new ArrayList<>(chunkSize);
            L[] rawArr = chunkCache[chunkIndex];

            if (rawArr == null || rawArr.length == 0) {
                return list; // Will have size 0
            }

            int added = 0;

            for (L l : rawArr) {
                if (l == null) {
                    continue;
                }

                list.add(l);

                if (++added == chunkSize) {
                    break;
                }
            }

            return list;
        }
    }

    private int indexOf(IntPosition position) {
        return indexOf(position.getChunkRelativeX(), position.y, position.getChunkRelativeZ());
    }

    private int indexOf(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    private int chunkIndex(ChunkCoords chunkCoords) {
        return chunkIndex(chunkCoords.getRegionRelativeX(), chunkCoords.getRegionRelativeZ());
    }

    private int chunkIndex(int cx, int cz) {
        return cz << 5 | cx;
    }

    @NotNull
    protected abstract L[] createArray(int size);

    @NotNull
    protected abstract L[][] createMultiArr(int size);

    @NotNull
    protected abstract L createInstance(IntPosition position, int lightLevel, boolean migrated, String material);

    /**
     * Unloads this Region, discarding any not-flushed changes.
     */
    public void unload() {
        Arrays.fill(chunkCache, null);
        Arrays.fill(chunkSizes, 0);

        dirtyChunks.clear();

        file.unload();
    }
}