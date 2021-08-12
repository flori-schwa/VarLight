package me.shawlaf.varlight.spigot.persistence;

import me.shawlaf.varlight.persistence.nls.common.exception.PositionOutOfBoundsException;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.Iterator;
import java.util.function.IntSupplier;

public interface ICustomLightStorage {

    World getForBukkitWorld();

    int getCustomLuminance(IntPosition position, int def);

    int getCustomLuminance(IntPosition position, IntSupplier def);

    boolean hasChunkCustomLightData(ChunkCoords chunkCoords);

    Iterator<IntPosition> iterateAllLightSources(IntPosition a, IntPosition b);

    default Iterator<IntPosition> iterateLightSources(ChunkCoords chunkCoords) {
        return iterateAllLightSources(chunkCoords.getChunkStart(), chunkCoords.getChunkEnd());
    }

    void setCustomLuminance(Location location, int luminance) throws PositionOutOfBoundsException;

    void setCustomLuminance(IntPosition position, int luminance) throws PositionOutOfBoundsException;

    void clearChunk(ChunkCoords chunkCoords);

    void runAutosave();

    void save(CommandSender commandSender, boolean log);
}
