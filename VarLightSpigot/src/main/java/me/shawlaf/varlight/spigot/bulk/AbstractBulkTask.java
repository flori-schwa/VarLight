package me.shawlaf.varlight.spigot.bulk;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.bulk.exception.BulkTaskTooLargeException;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractBulkTask {

    @NotNull
    protected final VarLightPlugin plugin;

    @NotNull
    protected final World world;
    @NotNull
    protected final CommandSender source;

    protected List<CommandSender> progressSubscribers;

    public AbstractBulkTask(@NotNull VarLightPlugin plugin, @NotNull World world, @NotNull CommandSender source) {
        this.plugin = Objects.requireNonNull(plugin);
        this.world = Objects.requireNonNull(world);
        this.source = Objects.requireNonNull(source);
    }

    public @NotNull World getWorld() {
        return world;
    }

    public @NotNull CommandSender getSource() {
        return source;
    }

    public void subscribeProgress(CommandSender subscriber) {
        this.progressSubscribers.add(subscriber);
    }

    public final CompletableFuture<BulkTaskResult> run() {
        if (forcePrimaryThread() && !Bukkit.isPrimaryThread()) {
            return plugin.getApi().getSyncExecutor().submit(this::doRun).join();
        }

        return doRun();
    }

    protected abstract boolean forcePrimaryThread();

    protected abstract CompletableFuture<BulkTaskResult> doRun();

    protected void checkSizeRestrictions(long affectedChunksCount) throws BulkTaskTooLargeException {
        final int limit = plugin.getVarLightConfig().getBulkChunkUpdateLimit();

        if (affectedChunksCount > limit) {
            throw new BulkTaskTooLargeException(limit, affectedChunksCount);
        }
    }

    protected CompletableFuture<Void> ticketChunks(Iterator<ChunkCoords> chunkCoords) {
        Runnable r = () -> {
            while (chunkCoords.hasNext()) {
                ChunkCoords next = chunkCoords.next();

                this.world.addPluginChunkTicket(next.x(), next.z(), plugin);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            r.run();
            return CompletableFuture.completedFuture(null);
        } else {
            return plugin.getApi().getSyncExecutor().submit(r, null);
        }
    }

    protected CompletableFuture<Void> releaseTickets(Iterator<ChunkCoords> chunkCoords) {
        Runnable r = () -> {
            while (chunkCoords.hasNext()) {
                ChunkCoords next = chunkCoords.next();

                this.world.removePluginChunkTicket(next.x(), next.z(), plugin);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            r.run();
            return CompletableFuture.completedFuture(null);
        } else {
            return plugin.getApi().getSyncExecutor().submit(r, null);
        }
    }


}
