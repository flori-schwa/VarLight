package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.nms.IMinecraftLightUpdater;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.World;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class LightUpdater implements IMinecraftLightUpdater {

    private final VarLightPlugin plugin;

    public LightUpdater(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> updateLightServer(World bukkitWorld, IntPosition position) throws VarLightNotActiveException {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateLightServer(World bukkitWorld, Collection<IntPosition> positions) throws VarLightNotActiveException {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateLightServer(World bukkitWorld, ChunkCoords chunk) throws VarLightNotActiveException {
        return null;
    }

    @Override
    public void updateLightClient(World world, ChunkCoords centerChunk) {

    }
}
