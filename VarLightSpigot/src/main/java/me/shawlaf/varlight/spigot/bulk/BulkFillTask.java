package me.shawlaf.varlight.spigot.bulk;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.command.result.CommandResultFailure;
import me.shawlaf.command.result.CommandResultSuccessBroadcast;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.api.LightUpdateResult;
import me.shawlaf.varlight.spigot.bulk.exception.BulkTaskTooLargeException;
import me.shawlaf.varlight.spigot.messages.VarLightMessages;
import me.shawlaf.varlight.spigot.permissions.tree.VarLightPermissionTree;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.spigot.progressbar.ProgressBar;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import me.shawlaf.varlight.util.pos.RegionIterator;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@ExtensionMethod({
        IntPositionExtension.class
})
public class BulkFillTask extends AbstractBulkTask {

    private static final int PROGRESS_BAR_THRESHOLD = 100_000;

    @NotNull
    @Getter
    private final IntPosition start;
    @NotNull
    @Getter
    private final IntPosition end;

    @Getter
    private int totalBlocks;

    @Getter
    private int lightLevel;

    @NotNull
    private final Predicate<Block> filter;

    private Set<IntPosition> illegalBlocks = new HashSet<>();
    private Set<IntPosition> skippedBlocks = new HashSet<>();
    private Set<IntPosition> failedBlocks = new HashSet<>();
    private Set<IntPosition> updatedBlocks = new HashSet<>();

    public BulkFillTask(@NotNull VarLightPlugin plugin, @NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end, int lightLevel, @Nullable Predicate<Block> filter) {
        super(plugin, world, source);

        Objects.requireNonNull(this.start = start);
        Objects.requireNonNull(this.end = end);

        this.lightLevel = lightLevel;
        this.filter = filter == null ? x -> true : filter;
    }

    @Override
    protected boolean forcePrimaryThread() {
        return true;
    }

    @Override
    public CompletableFuture<BulkTaskResult> doRun() {
        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(this.world)) == null) {
            return CompletableFuture.completedFuture(new BulkTaskResult(this, BulkTaskResult.Type.NOT_ACTIVE, new CommandResultFailure(plugin.getCommand(), VarLightMessages.varLightNotActiveInWorld(this.world))));
        }

        RegionIterator regionIterator = new RegionIterator(this.start, this.end);

        Set<ChunkCoords> affectedChunks = regionIterator.getAllContainingChunks();

        try {
            checkSizeRestrictions(affectedChunks);
        } catch (BulkTaskTooLargeException e) {
            return CompletableFuture.completedFuture(
                    new BulkTaskResult(this,
                            BulkTaskResult.Type.TOO_LARGE,
                            new CommandResultFailure(
                                    plugin.getCommand(),
                                    String.format("The fill command may only affect a maximum of %d chunks, you are trying to manipulate an area affecting %d chunks.", e.getChunkLimit(), e.getNChunksTryingToModify())
                            )
                    )
            );
        }

        boolean useProgressBar = regionIterator.getSize() >= PROGRESS_BAR_THRESHOLD;

        ticketChunks(regionIterator.iterateChunks()).join();

        return plugin.getApi().getAsyncExecutor().submit(() -> {
            try {
                plugin.getApi().getSyncExecutor().submit(() -> {
                    try (ProgressBar progressBar = useProgressBar ? new ProgressBar(plugin, "VarLight fill | Iterating Blocks", regionIterator.getSize()) : ProgressBar.VOID) {
                        progressBar.subscribeAll(this.progressSubscribers);

                        IntPosition next;

                        while (regionIterator.hasNext()) {
                            next = regionIterator.next();
                            Block block = next.toBlock(this.world);
                            ++totalBlocks;
                            progressBar.step();

                            if (!filter.test(block)) {
                                skippedBlocks.add(next);
                                continue;
                            }

                            if (plugin.getNmsAdapter().isIllegalBlock(block)) {
                                illegalBlocks.add(next);
                                continue;
                            }

                            LightUpdateResult result = plugin.getApi().setCustomLuminance(this.world, next, this.lightLevel, false).join();

                            if (!result.isSuccess()) {
                                failedBlocks.add(next);
                            } else {
                                updatedBlocks.add(next);
                            }
                        }
                    }
                }).join();

                plugin.getLightUpdater().updateLightMultiBlock(cls, updatedBlocks, useProgressBar ? this.progressSubscribers : null).join();

                return new BulkTaskResult(this, BulkTaskResult.Type.SUCCESS,
                        new CommandResultSuccessBroadcast(plugin.getCommand(),
                                String.format("Successfully updated %d Light sources in Region %s to %s. (Total blocks: %d, Invalid Blocks: %d, Skipped Blocks: %d, Failed Blocks: %d)",
                                        updatedBlocks.size(),
                                        this.start.toShortString(),
                                        this.end.toShortString(),
                                        totalBlocks,
                                        illegalBlocks.size(),
                                        skippedBlocks.size(),
                                        failedBlocks.size()
                                ), VarLightPermissionTree.MODIFY));
            } catch (Exception e) {
                e.printStackTrace();
                return new BulkTaskResult(this, BulkTaskResult.Type.ERROR, new CommandResultFailure(plugin.getCommand(), String.format("Failed to run fill command: %s", e.getMessage())));
            } finally {
                releaseTickets(regionIterator.iterateChunks()).join();
            }
        });
    }


}
