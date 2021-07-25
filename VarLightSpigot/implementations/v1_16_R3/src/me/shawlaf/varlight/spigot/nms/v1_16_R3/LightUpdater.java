package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.IMinecraftLightUpdater;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.spigot.progressbar.ProgressBar;
import me.shawlaf.varlight.spigot.util.RegionIterator;
import me.shawlaf.varlight.spigot.util.collections.IteratorUtils;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.Tuple;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.joor.Reflect;

import java.util.Collection;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ExtensionMethod({
        Util.class,
        IteratorUtils.class
})
public class LightUpdater implements IMinecraftLightUpdater, Listener {

    private final VarLightPlugin plugin;

    public LightUpdater(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (World world : plugin.getVarLightConfig().getVarLightEnabledWorlds()) {
            injectCustomILightAccess(world);
        }
    }

    private boolean isLightSource(Tuple<IChunkAccess, ICustomLightStorage> data, IntPosition position) {
        return data.item1.getType(position.toBlockPosition()).f() != 0 || data.item2.getCustomLuminance(position, 0) != 0;
    }

    private Stream<IntPosition> streamChunkBlocks(ICustomLightStorage cls, ChunkCoords chunkPos) {
        return streamChunkBlocks(cls,chunkPos, (tup, pos) -> true);
    }

    private Stream<IntPosition> streamChunkBlocks(ICustomLightStorage cls, ChunkCoords chunkPos, BiPredicate<Tuple<IChunkAccess, ICustomLightStorage>, IntPosition> pred) {
        if (!Bukkit.isPrimaryThread()) {
            return plugin.getApi().getSyncExecutor().submit(() -> streamChunkBlocks(cls, chunkPos, pred)).join();
        }

        WorldServer world = cls.getForBukkitWorld().toNmsWorld();
        IChunkAccess chunk = world.getChunkProvider().a(chunkPos.x, chunkPos.z);

        if (chunk == null) {
            return Stream.of(); // Empty
        }

        Tuple<IChunkAccess, ICustomLightStorage> tuple = new Tuple<>(chunk, cls);
        RegionIterator it = new RegionIterator(chunkPos.getChunkStart(), chunkPos.getChunkEnd());

        return StreamSupport.stream(
                Spliterators.spliterator(
                        it,
                        it.getSize(),
                        Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.SIZED
                ),
                false
        ).filter(pos -> pred.test(tuple, pos));
    }

    private CompletableFuture<Void> joinAllWithProgress(String name, CompletableFuture<?>[] futures, Collection<CommandSender> progressSubscribers) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Runnable r = () -> {
            try (ProgressBar progressBar = nullOrEmpty(progressSubscribers) ? ProgressBar.VOID : new ProgressBar(plugin, name, futures.length)) {
                progressBar.subscribeAll(progressSubscribers);

                for (int i = 0; i < futures.length; i++) {
                    futures[i].join();
                    progressBar.step();
                }
            }

            future.complete(null);
        };

        if (!Bukkit.isPrimaryThread()) {
            r.run();
        } else {
            plugin.getApi().getAsyncExecutor().submit(r).join();
        }

        return future;
    }

    private CompletableFuture<IChunkAccess>[] lightChunks(WorldServer world, Collection<ChunkCoords> chunks) {
        CompletableFuture<IChunkAccess>[] futures = new CompletableFuture[chunks.size()];
        LightEngineThreaded let = ((LightEngineThreaded) world.getLightProvider());

        Runnable r = () -> {
            int i = 0;
            for (ChunkCoords coords : chunks) {
                IChunkAccess chunk = world.getChunkProvider().a(coords.x, coords.z);

                if (chunk == null) {
                    futures[i++] = CompletableFuture.completedFuture(null);
                    continue;
                }

                futures[i++] = let.lightChunk(chunk, true);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            r.run();
        } else {
            plugin.getApi().getSyncExecutor().submit(r).join();
        }

        return futures;

    }

    private CompletableFuture<Void> sendClientUpdates(WorldServer world, Collection<ChunkCoords> chunks, Collection<CommandSender> progressSubscribers) {
        LightEngineThreaded let = ((LightEngineThreaded) world.getLightProvider());

        Runnable r = () -> {
            try (ProgressBar progressBar = nullOrEmpty(progressSubscribers) ? ProgressBar.VOID : new ProgressBar(plugin, "Send Client Updates", chunks.size())) {
                progressBar.subscribeAll(progressSubscribers);

                for (ChunkCoords chunkCoords : chunks) {
                    ChunkCoordIntPair coords = new ChunkCoordIntPair(chunkCoords.x, chunkCoords.z);
                    PacketPlayOutLightUpdate ppolu = new PacketPlayOutLightUpdate(coords, let, false);

                    world.getChunkProvider().playerChunkMap.a(coords, false).forEach(
                            ep -> ep.playerConnection.sendPacket(ppolu)
                    );

                    progressBar.step();
                }
            }
        };

        if (Bukkit.isPrimaryThread()) {
            r.run();
            return CompletableFuture.completedFuture(null);
        }

        return plugin.getApi().getSyncExecutor().submit(r, null);
    }

    @Override
    public CompletableFuture<Void> updateLightSingleBlock(ICustomLightStorage lightStorage, IntPosition position) {
        World bukkitWorld = lightStorage.getForBukkitWorld();
        WorldServer nmsWorld = bukkitWorld.toNmsWorld();

        LightEngineThreaded let = ((LightEngineThreaded) nmsWorld.getLightProvider());

        return plugin.getApi().getAsyncExecutor().submit(() -> {
            nmsWorld.runLightEngineSync(() -> ((LightEngineBlock) let.getLightingView(EnumSkyBlock.BLOCK)).checkBlock(position)).join();

            Set<ChunkCoords> chunksToUpdate = RegionIterator.squareChunkArea(position.toChunkCoords(), 1).collectFromIterator(Collectors.toSet());

            joinAllWithProgress("Light Chunks", lightChunks(nmsWorld, chunksToUpdate), null).join();
            sendClientUpdates(nmsWorld, chunksToUpdate, null).join();
        }, null);
    }

    @Override
    public CompletableFuture<Void> updateLightMultiBlock(ICustomLightStorage lightStorage, Collection<IntPosition> positions, Collection<CommandSender> progressSubscribers) {
        WorldServer nmsWorld = lightStorage.getForBukkitWorld().toNmsWorld();

        LightEngineThreaded let = ((LightEngineThreaded) nmsWorld.getLightProvider());
        LightEngineBlock leb = ((LightEngineBlock) let.getLightingView(EnumSkyBlock.BLOCK));

        return plugin.getApi().getAsyncExecutor().submit(() -> {
            let.runLightEngineSync(() -> {
                try (ProgressBar progressBar = nullOrEmpty(progressSubscribers) ? ProgressBar.VOID : new ProgressBar(plugin, "Update Light", positions.size())) {
                    progressSubscribers.forEach(progressBar::subscribe);

                    for (IntPosition position : positions) {
                        leb.checkBlock(position);
                        progressBar.step();
                    }
                }
            }).join();

            Set<ChunkCoords> affectedChunks = positions.parallelStream().map(IntPosition::toChunkCoords).collect(Collectors.toSet());
            Set<ChunkCoords> toUpdate = affectedChunks.parallelStream()
                    .map(c -> RegionIterator.squareChunkArea(c, 1))
                    .flatMap(it -> IteratorUtils.collectFromIterator(it, Collectors.toSet()).stream())
                    .collect(Collectors.toSet());

            joinAllWithProgress("Light Chunks", lightChunks(nmsWorld, toUpdate), progressSubscribers).join();
            sendClientUpdates(nmsWorld, toUpdate, progressSubscribers).join();
        }, null);
    }

    @Override
    public CompletableFuture<Void> updateLightChunk(ICustomLightStorage lightStorage, ChunkCoords center, Collection<CommandSender> progressSubscribers) {
        WorldServer nmsWorld = lightStorage.getForBukkitWorld().toNmsWorld();
        LightEngineThreaded let = ((LightEngineThreaded) nmsWorld.getLightProvider());
        LightEngineBlock leb = ((LightEngineBlock) let.getLightingView(EnumSkyBlock.BLOCK));

        return plugin.getApi().getAsyncExecutor().submit(() -> {
            Set<ChunkCoords> toUpdate = RegionIterator.squareChunkArea(center, 1).collectFromIterator(Collectors.toSet());

            let.runLightEngineSync(() -> {
                try (ProgressBar progressBar = nullOrEmpty(progressSubscribers) ? ProgressBar.VOID : new ProgressBar(plugin, "Update Light", toUpdate.size())) {
                    progressBar.subscribeAll(progressSubscribers);

                    for (ChunkCoords chunkCoords : toUpdate) {
                        streamChunkBlocks(lightStorage, chunkCoords, this::isLightSource).forEach((pos) -> leb.checkBlock(pos));
                        progressBar.step();
                    }
                }
            }).join();

            joinAllWithProgress("Light Chunks", lightChunks(nmsWorld, toUpdate), progressSubscribers).join();
            sendClientUpdates(nmsWorld, toUpdate, progressSubscribers).join();
        }, null);
    }

    @Override
    public CompletableFuture<Void> updateLightMultiChunk(ICustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions, Collection<CommandSender> progressSubscribers) {
        WorldServer nmsWorld = lightStorage.getForBukkitWorld().toNmsWorld();
        LightEngineThreaded let = ((LightEngineThreaded) nmsWorld.getLightProvider());
        LightEngineBlock leb = ((LightEngineBlock) let.getLightingView(EnumSkyBlock.BLOCK));

        return plugin.getApi().getAsyncExecutor().submit(() -> {
            Set<ChunkCoords> toUpdate = chunkPositions.parallelStream()
                    .map(c -> RegionIterator.squareChunkArea(c, 1))
                    .flatMap(it -> IteratorUtils.collectFromIterator(it, Collectors.toSet()).stream())
                    .collect(Collectors.toSet());

            let.runLightEngineSync(() -> {
                try (ProgressBar progressBar = nullOrEmpty(progressSubscribers) ? ProgressBar.VOID : new ProgressBar(plugin, "Update Light", toUpdate.size())) {
                    progressBar.subscribeAll(progressSubscribers);

                    for (ChunkCoords chunkCoords : toUpdate) {
                        streamChunkBlocks(lightStorage, chunkCoords, this::isLightSource).forEach((pos) -> leb.checkBlock(pos));
                        progressBar.step();
                    }
                }
            }).join();

            joinAllWithProgress("Light Chunks", lightChunks(nmsWorld, toUpdate), progressSubscribers).join();
            sendClientUpdates(nmsWorld, toUpdate, progressSubscribers).join();
        }, null);
    }

    @Override
    public CompletableFuture<Void> clearLightChunk(ICustomLightStorage lightStorage, ChunkCoords center, Collection<CommandSender> progressSubscribers) {
        WorldServer nmsWorld = lightStorage.getForBukkitWorld().toNmsWorld();
        LightEngineThreaded let = ((LightEngineThreaded) nmsWorld.getLightProvider());
        LightEngineBlock leb = ((LightEngineBlock) let.getLightingView(EnumSkyBlock.BLOCK));

        return plugin.getApi().getAsyncExecutor().submit(() -> {
            Set<ChunkCoords> toUpdate = RegionIterator.squareChunkArea(center, 1).collectFromIterator(Collectors.toSet());

            let.runLightEngineSync(() -> {
                try (ProgressBar progressBar = nullOrEmpty(progressSubscribers) ? ProgressBar.VOID : new ProgressBar(plugin, "Clear Light", toUpdate.size())) {
                    progressBar.subscribeAll(progressSubscribers);

                    for (ChunkCoords chunkCoords : toUpdate) {
                        streamChunkBlocks(lightStorage, chunkCoords).forEach((pos) -> leb.checkBlock(pos));
                        progressBar.step();
                    }
                }
            }).join();

            joinAllWithProgress("Light Chunks", lightChunks(nmsWorld, toUpdate), progressSubscribers).join();
            sendClientUpdates(nmsWorld, toUpdate, progressSubscribers).join();
        }, null);
    }

    @Override
    public CompletableFuture<Void> clearLightMultiChunk(ICustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions, Collection<CommandSender> progressSubscribers) {
        WorldServer nmsWorld = lightStorage.getForBukkitWorld().toNmsWorld();
        LightEngineThreaded let = ((LightEngineThreaded) nmsWorld.getLightProvider());
        LightEngineBlock leb = ((LightEngineBlock) let.getLightingView(EnumSkyBlock.BLOCK));

        return plugin.getApi().getAsyncExecutor().submit(() -> {
            Set<ChunkCoords> toUpdate = chunkPositions.parallelStream()
                    .map(c -> RegionIterator.squareChunkArea(c, 1))
                    .flatMap(it -> IteratorUtils.collectFromIterator(it, Collectors.toSet()).stream())
                    .collect(Collectors.toSet());

            let.runLightEngineSync(() -> {
                try (ProgressBar progressBar = nullOrEmpty(progressSubscribers) ? ProgressBar.VOID : new ProgressBar(plugin, "Clear Light", toUpdate.size())) {
                    progressBar.subscribeAll(progressSubscribers);

                    for (ChunkCoords chunkCoords : toUpdate) {
                        streamChunkBlocks(lightStorage, chunkCoords).forEach((pos) -> leb.checkBlock(pos));
                        progressBar.step();
                    }
                }
            }).join();

            joinAllWithProgress("Light Chunks", lightChunks(nmsWorld, toUpdate), progressSubscribers).join();
            sendClientUpdates(nmsWorld, toUpdate, progressSubscribers).join();
        }, null);
    }

    @Override
    public VarLightPlugin getPlugin() {
        return plugin;
    }

    private void injectCustomILightAccess(World bukkitWorld) {
        WorldServer nmsWorld = bukkitWorld.toNmsWorld();

        Reflect.on(
                nmsWorld.getLightProvider().getLightingView(EnumSkyBlock.BLOCK)
        ).set("a", new WrappedLightAccess(plugin, nmsWorld));
    }

    private boolean nullOrEmpty(Collection<?> x) {
        return x == null || x.isEmpty();
    }

    // region Events

    @EventHandler
    private void onWorldLoadEvent(WorldLoadEvent e) {
        if (plugin.getVarLightConfig().getVarLightEnabledWorldNames().contains(e.getWorld().getName())) {
            injectCustomILightAccess(e.getWorld());
        }
    }

    // endregion
}
