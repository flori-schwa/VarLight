package me.shawlaf.varlight.spigot.bulk;

import lombok.Getter;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class AbstractBulkTask {

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

    // TODO force Load chunks

    protected CompletableFuture<Void> ticketChunks(Iterator<ChunkCoords> chunkCoords) {
        return CompletableFuture.completedFuture(null);
    }

    protected CompletableFuture<Void> releaseTickets(Iterator<ChunkCoords> chunkCoords) {
        return CompletableFuture.completedFuture(null);
    }


}
