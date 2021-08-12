package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import org.bukkit.Chunk;

@UtilityClass
public class ChunkCoordExtension {

    public ChunkCoords toChunkCoords(Chunk chunk) {
        return new ChunkCoords(chunk.getX(), chunk.getZ());
    }

}
