package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * An Interface, that defines methods for updating Light in Minecraft Internally
 */
public interface IMinecraftLightUpdater extends IPluginLifeCycleOperations {

    CompletableFuture<Void> updateLightSingleBlock(ICustomLightStorage lightStorage, IntPosition position);

    CompletableFuture<Void> updateLightMultiBlock(ICustomLightStorage lightStorage, Collection<IntPosition> positions, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Void> updateLightChunk(ICustomLightStorage lightStorage, ChunkCoords chunk, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Void> updateLightMultiChunk(ICustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Set<IntPosition>> clearLightCubicArea(ICustomLightStorage lightStorage, IntPosition start, IntPosition end, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Void> clearLightChunk(ICustomLightStorage lightStorage, ChunkCoords chunk, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Void> clearLightMultiChunk(ICustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions, Collection<CommandSender> progressSubscribers);


    default CompletableFuture<Void> updateLightChunk(ICustomLightStorage lightStorage, ChunkCoords chunk) {
        return updateLightChunk(lightStorage, chunk, null);
    }

    default CompletableFuture<Void> updateLightMultiChunk(ICustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions) {
        return updateLightMultiChunk(lightStorage, chunkPositions, null);
    }

    VarLightPlugin getPlugin();

}
