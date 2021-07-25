package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public interface IChunkCustomLightAccess {

    int getCustomLuminance(IntPosition position);

    void setCustomLuminance(IntPosition position, int value);

    ChunkCoords getChunkPosition();

    boolean hasData();

    void clear();

    int getMask();

    @NotNull Iterator<IntPosition> iterateLightSources();

    @Deprecated @NotNull List<IntPosition> getAllLightSources();

}