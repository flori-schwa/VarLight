package me.shawlaf.varlight.spigot.bulk;

import me.shawlaf.command.result.CommandResultFailure;
import me.shawlaf.command.result.CommandResultSuccessBroadcast;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.bulk.exception.BulkTaskTooLargeException;
import me.shawlaf.varlight.spigot.messages.VarLightMessages;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.util.collections.IteratorUtils;
import me.shawlaf.varlight.util.pos.ChunkPosition;
import me.shawlaf.varlight.util.pos.IntPosition;
import me.shawlaf.varlight.util.pos.RegionIterator;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BulkClearTask extends AbstractBulkTask {

    private static final int PROGRESS_BAR_THRESHOLD = 100_000;

    @NotNull
    private final IntPosition start;
    @NotNull
    private final IntPosition end;
    private Set<IntPosition> clearedLightSources;

    public BulkClearTask(@NotNull VarLightPlugin plugin, @NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end) {
        super(plugin, world, source);

        this.start = start;
        this.end = end;
    }

    public @NotNull IntPosition getStart() {
        return start;
    }

    public @NotNull IntPosition getEnd() {
        return end;
    }

    public Set<IntPosition> getClearedLightSources() {
        return clearedLightSources;
    }

    @Override
    protected boolean forcePrimaryThread() {
        return true;
    }

    @Override
    public CompletableFuture<BulkTaskResult> doRun() {
        RegionIterator iterator = new RegionIterator(this.start, this.end);
        Set<ChunkPosition> affectedChunks = IteratorUtils.collectFromIterator(iterator.iterateChunks(), Collectors.toSet());

        try {
            checkSizeRestrictions(iterator.getAffectedChunksCount());
        } catch (BulkTaskTooLargeException e) {
            return CompletableFuture.completedFuture(
                    new BulkTaskResult(this,
                            BulkTaskResult.Type.TOO_LARGE,
                            new CommandResultFailure(
                                    plugin.getCommand(),
                                    String.format("The clear command may only affect a maximum of %d chunks, you are trying to manipulate an area affecting %d chunks.", e.getChunkLimit(), e.getAmountOfChunksTryingToModify())
                            )
                    )
            );
        }

        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(this.world)) == null) {
            return CompletableFuture.completedFuture(new BulkTaskResult(this, BulkTaskResult.Type.NOT_ACTIVE, new CommandResultFailure(plugin.getCommand(), VarLightMessages.varLightNotActiveInWorld(this.world))));
        }

        return plugin.getApi().getAsyncExecutor().submit(() -> {
            try {
                ticketChunks(iterator.iterateChunks()).join();

                this.clearedLightSources = plugin.getLightUpdater().clearLightCubicArea(cls, this.start, this.end, iterator.getSize() >= PROGRESS_BAR_THRESHOLD ? progressSubscribers : null).join();

                return new BulkTaskResult(this, BulkTaskResult.Type.SUCCESS, new CommandResultSuccessBroadcast(
                        plugin.getCommand(),
                        String.format("Cleared Custom Light Data from %s to %s (%d chunks)", start.toShortString(), end.toShortString(), affectedChunks.size())
                ));
            } catch (Exception e) {
                return new BulkTaskResult(this, BulkTaskResult.Type.ERROR, new CommandResultFailure(plugin.getCommand(), e.getMessage()));
            } finally {
                releaseTickets(iterator.iterateChunks());
            }
        });
    }
}
