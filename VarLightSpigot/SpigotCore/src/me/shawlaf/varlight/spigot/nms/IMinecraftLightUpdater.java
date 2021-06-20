package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import me.shawlaf.varlight.spigot.persistence.CustomLightStorage;
import me.shawlaf.varlight.spigot.util.ChunkCoordExtension;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An Interface, that defines methods for updating Light in Minecraft Internally
 */
public interface IMinecraftLightUpdater extends IPluginLifeCycleOperations {

    CompletableFuture<Void> updateLightFull(CustomLightStorage lightStorage, IntPosition position);

    default CompletableFuture<Void> updateLightFull(World bukkitWorld, IntPosition position) throws VarLightNotActiveException {
        return updateLightFull(getPlugin().getApi().requireVarLightEnabled(bukkitWorld), position);
    }

    /**
     * <p>
     * Updates the Light Level at the specified Block location in the specified World, using the custom luminance stored in NLS
     * This method will and should not:
     * </p>
     *
     * <ul>
     *     <li>Send light updates to connected clients, use {@link IMinecraftLightUpdater#updateLightClient(World, ChunkCoords) } for that</li>
     *     <li>Update the Custom Light Level stored by VarLight, use {@link CustomLightStorage} for that</li>
     * </ul>
     *
     * @param bukkitWorld The {@link World} to update Light in
     * @param position    The {@link IntPosition} of the Light update
     * @return A {@link CompletableFuture} representing pending completion of the Light update Task
     * @throws VarLightNotActiveException When VarLight is not active in the specified {@link World}
     */
    CompletableFuture<Void> updateLightServer(World bukkitWorld, IntPosition position) throws VarLightNotActiveException;

    /**
     * Updates the Light at all specified Block locations in the specified World
     *
     * @param bukkitWorld The {@link World} to update Light in
     * @param positions   A {@link Collection} of positions to update
     * @return A {@link CompletableFuture} representing pending completion of the Light update Task
     * @throws VarLightNotActiveException When VarLight is not active in the specified {@link World}
     * @see IMinecraftLightUpdater#updateLightServer(World, IntPosition)
     */
    CompletableFuture<Void> updateLightServer(World bukkitWorld, Collection<IntPosition> positions) throws VarLightNotActiveException;

    CompletableFuture<Void> updateLightServer(CustomLightStorage lightStorage, ChunkCoords chunk);

    /**
     * Updates the Light at all Blocks in the Chunk where the custom stored luminance in NLS is not 0.
     *
     * @param bukkitWorld The {@link World} to update Light in
     * @param chunk       The Chunk to look for custom Light sources in
     * @return A {@link CompletableFuture} representing pending completion of the Light update Task
     * @throws VarLightNotActiveException When VarLight is not active in the specified {@link World}
     * @see IMinecraftLightUpdater#updateLightServer(World, IntPosition)
     */
    default CompletableFuture<Void> updateLightServer(World bukkitWorld, ChunkCoords chunk) throws VarLightNotActiveException {
        return updateLightServer(getPlugin().getApi().requireVarLightEnabled(bukkitWorld), chunk);
    }

    void updateLightClient(CustomLightStorage lightStorage, ChunkCoords center);

    /**
     * Sends Light update Packets in a 3x3 area around the center Chunk to connected Clients
     *
     * @param world       The {@link World} containing the Chunk
     * @param centerChunk The coordinates of the center Chunk
     */
    default void updateLightClient(World world, ChunkCoords centerChunk) throws VarLightNotActiveException {
        updateLightClient(getPlugin().getApi().requireVarLightEnabled(world), centerChunk);
    }

    /**
     * @param location The {@link Location} of the Light update
     * @return A {@link CompletableFuture} representing pending completion of the Light update Task
     * @throws VarLightNotActiveException When VarLight is not active in the world given by {@link Location#getWorld()}
     * @see IMinecraftLightUpdater#updateLightServer(World, IntPosition)
     */
    default CompletableFuture<Void> updateLightServer(Location location) throws VarLightNotActiveException {
        return updateLightServer(location.getWorld(), IntPositionExtension.toIntPosition(location));
    }

    /**
     * @param chunk The {@link Chunk} to look for custom Light sources in
     * @return A {@link CompletableFuture} representing pending completion of the Light update Task
     * @throws VarLightNotActiveException When VarLight is not active in the world given by {@link Chunk#getWorld()}
     * @see IMinecraftLightUpdater#updateLightServer(World, ChunkCoords)
     */
    default CompletableFuture<Void> updateLightServer(Chunk chunk) throws VarLightNotActiveException {
        return updateLightServer(chunk.getWorld(), ChunkCoordExtension.toChunkCoords(chunk));
    }

    default void updateLightClient(Chunk chunk) throws VarLightNotActiveException {
        updateLightClient(chunk.getWorld(), ChunkCoordExtension.toChunkCoords(chunk));
    }

    VarLightPlugin getPlugin();

}
