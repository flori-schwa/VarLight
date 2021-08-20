package me.shawlaf.varlight.spigot.bulk;

import lombok.Getter;
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
    @Getter
    protected final World world;
    @NotNull
    @Getter
    protected final CommandSender source;

    protected List<CommandSender> progressSubscribers;

    public AbstractBulkTask(@NotNull VarLightPlugin plugin, @NotNull World world, @NotNull CommandSender source) {
        Objects.requireNonNull(this.plugin = plugin);
        Objects.requireNonNull(this.world = world);
        Objects.requireNonNull(this.source = source);
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

    protected void checkSizeRestrictions(Set<ChunkCoords> affectedChunks) throws BulkTaskTooLargeException {
        final int limit = 25; // TODO Make configurable

        if (affectedChunks.size() > 25) {
            throw new BulkTaskTooLargeException(limit, affectedChunks.size());
        }
    }

    protected CompletableFuture<Void> ticketChunks(Iterator<ChunkCoords> chunkCoords) {
        Runnable r = () -> {
            while (chunkCoords.hasNext()) {
                ChunkCoords next = chunkCoords.next();

                this.world.addPluginChunkTicket(next.x, next.z, plugin);
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

                this.world.removePluginChunkTicket(next.x, next.z, plugin);
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
