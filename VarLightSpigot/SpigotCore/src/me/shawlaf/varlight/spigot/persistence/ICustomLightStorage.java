package me.shawlaf.varlight.spigot.persistence;

import me.shawlaf.varlight.persistence.nls.common.exception.PositionOutOfBoundsException;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Stores Custom Light Levels per World
 */
public interface ICustomLightStorage {

    /**
     * @return The World this handler stores custom Light Levels for
     */
    World getForBukkitWorld();

    /**
     * Get the stored Custom Light Level at the given position
     * @param position The position to query for Custom Light Data
     * @return The Custom Light Level at the given Position, or {@code 0} if none present
     */
    default int getCustomLuminance(IntPosition position) {
        return getCustomLuminance(position);
    }

    /**
     * Get the stored Custom Light Level at the given position
     * @param position The position to query for Custom Light Data
     * @param def The default value to return if no custom Light Data is present
     * @return The Custom Light Level at the given Position, or {@code def} if none present
     */
    int getCustomLuminance(IntPosition position, int def);

    /**
     * Check if the given Chunk contains Custom Light Data
     * @param chunkCoords The coordinates of the Chunk to query
     * @return {@code true}, if the Chunk contains Custom Light data, {@code false} otherwise
     */
    boolean hasChunkCustomLightData(ChunkCoords chunkCoords);

    /**
     * @param a The start Position
     * @param b The end Position
     * @return an {@link Iterator} returning {@link IntPosition}s of all Custom Light Sources in the specified cubic area
     */
    Iterator<IntPosition> iterateAllLightSources(IntPosition a, IntPosition b);

    /**
     * @param chunkCoords The Chunk Coordinates to query for all Custom Light Sources
     * @return an {@link Iterator} returning {@link IntPosition}s for all Custom Light Sources in the specified Chunk
     */
    default Iterator<IntPosition> iterateLightSources(ChunkCoords chunkCoords) {
        return iterateAllLightSources(chunkCoords.getChunkStart(), chunkCoords.getChunkEnd());
    }

    /**
     * Set the Custom Light Data at the given position
     * @param location The {@link Location} of the Custom Light Source
     * @param luminance The Light Level that should be saved
     * @throws PositionOutOfBoundsException if the Position is out of bounds of the underlying {@link World}
     * @return The old Custom Light Level
     */
    int setCustomLuminance(Location location, int luminance) throws PositionOutOfBoundsException;

    /**
     * Set the Custom Light Data at the given position
     * @param position The {@link IntPosition} of the Custom Light Source
     * @param luminance The Light Level that should be saved
     * @throws PositionOutOfBoundsException if the Position is out of bounds of the underlying {@link World}
     * @return The old Custom Light Level
     */
    int setCustomLuminance(IntPosition position, int luminance) throws PositionOutOfBoundsException;

    /**
     * Removes all Custom Light Data from the specified Chunk
     * @param chunkCoords The Coordinates of the Chunk to clear
     */
    void clearChunk(ChunkCoords chunkCoords);

    /**
     * Used by the {@link Autosave} handler to automatically persist all Custom Light Sources in the underlying {@link World}
     */
    void runAutosave();

    /**
     * Save all Custom Light Sources in the underlying {@link World}
     *
     * @param commandSender The source of the Save, can be {@code null}
     * @param log Whether or not the specified {@link CommandSender} should be notified after the save is complete
     */
    void save(@Nullable CommandSender commandSender, boolean log);
}
