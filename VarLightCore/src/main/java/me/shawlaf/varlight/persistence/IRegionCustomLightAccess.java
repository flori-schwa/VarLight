package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public interface IRegionCustomLightAccess {

    int getCustomLuminance(IntPosition position);

    int setCustomLuminance(IntPosition position, int value);

    int getNonEmptyChunks();

    default boolean hasChunkData(ChunkCoords chunkCoords) {
        return Optional.ofNullable(getChunk(chunkCoords)).map(IChunkCustomLightAccess::hasData).orElse(false);
    }

    default void clearChunk(ChunkCoords chunkCoords) {
        Optional.ofNullable(getChunk(chunkCoords)).ifPresent(IChunkCustomLightAccess::clear);
    }

    int getMask(ChunkCoords chunkCoords);

    @Nullable IChunkCustomLightAccess getChunk(ChunkCoords chunkCoords);

    @NotNull List<ChunkCoords> getAffectedChunks();

    default @NotNull Iterator<IntPosition> iterateLightSources(ChunkCoords chunkCoords) {
        return Optional.ofNullable(getChunk(chunkCoords)).map(IChunkCustomLightAccess::iterateLightSources).orElse(Collections.emptyIterator());
    }

    @NotNull Iterator<IntPosition> iterateAllLightSources();

}
