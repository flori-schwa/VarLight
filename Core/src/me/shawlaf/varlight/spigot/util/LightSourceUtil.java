package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.spigot.LightUpdateResult;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.event.LightUpdateEvent;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static me.shawlaf.varlight.spigot.LightUpdateResult.*;

@UtilityClass
public class LightSourceUtil {

    public static LightUpdateResult placeNewLightSource(VarLightPlugin plugin, CommandSender source, Location location, int lightLevel) {
        return placeNewLightSource(plugin, source, location, lightLevel, true);
    }

    public static LightUpdateResult placeNewLightSource(VarLightPlugin plugin, CommandSender source, Location location, int lightLevel, boolean doUpdate) {

        IntPosition position = IntPositionExtension.toIntPosition(location);
        ChunkCoords center = position.toChunkCoords();

        int fromLight = location.getBlock().getLightFromBlocks();

        WorldLightSourceManager manager = plugin.getManager(Objects.requireNonNull(location.getWorld()));

        if (manager == null) {
            return varLightNotActive(plugin, location.getWorld(), fromLight, lightLevel);
        }

        fromLight = manager.getCustomLuminance(position, 0);

        if (lightLevel < 0) {
            return zeroReached(plugin, fromLight, lightLevel);
        }

        if (lightLevel > 15) {
            return fifteenReached(plugin, fromLight, lightLevel);
        }

        if (plugin.getNmsAdapter().isIllegalBlock(location.getBlock())) {
            return invalidBlock(plugin, fromLight, lightLevel);
        }

        LightUpdateEvent lightUpdateEvent = new LightUpdateEvent(source, location.getBlock(), fromLight, lightLevel, !Bukkit.getServer().isPrimaryThread());
        Bukkit.getPluginManager().callEvent(lightUpdateEvent);

        if (lightUpdateEvent.isCancelled()) {
            return cancelled(plugin, fromLight, lightUpdateEvent.getToLight());
        }

        int lightTo = lightUpdateEvent.getToLight();

        manager.setCustomLuminance(location, lightTo);

        if (doUpdate) {
            plugin.getNmsAdapter().updateBlock(location).thenRunAsync(
                    () -> {
                        Collection<ChunkCoords> neighbours = plugin.getNmsAdapter().collectChunkPositionsToUpdate(position);
                        List<CompletableFuture<Void>> futures = new ArrayList<>(neighbours.size());

                        for (ChunkCoords neighbour : neighbours) {
                            futures.add(plugin.getNmsAdapter().updateChunk(location.getWorld(), neighbour));
                        }

                        plugin.getBukkitAsyncExecutorService().submit(() -> {
                            futures.forEach(CompletableFuture::join);
                        }).thenRunAsync(() -> plugin.getNmsAdapter().sendLightUpdates(location.getWorld(), center), plugin.getBukkitMainThreadExecutorService());
                    }, plugin.getBukkitMainThreadExecutorService()
            );
        }

        return updated(plugin, fromLight, lightTo);
    }
}
