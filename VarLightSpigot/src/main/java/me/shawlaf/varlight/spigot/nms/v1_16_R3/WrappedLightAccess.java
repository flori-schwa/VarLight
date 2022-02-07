package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WrappedLightAccess implements ILightAccess, Listener {

    private final Map<ChunkCoords, WrappedIChunkAccess> proxies = Collections.synchronizedMap(new HashMap<>());

    private final VarLightPlugin plugin;
    private final WorldServer world;

    public WrappedLightAccess(VarLightPlugin plugin, WorldServer world) {
        this.plugin = plugin;
        this.world = world;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private int getCustomLuminance(IChunkAccess blockAccess, BlockPosition bPos) {
        int vanilla = blockAccess.getType(bPos).f();

        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(this.world.getWorld())) == null) {
            return vanilla;
        }

        return cls.getCustomLuminance(new IntPosition(bPos.getX(), bPos.getY(), bPos.getZ()), vanilla);
    }

    private WrappedIChunkAccess createProxy(ChunkCoords chunkCoords) {
        IChunkAccess toWrap = (IChunkAccess) getWrapped().c(chunkCoords.x, chunkCoords.z);

        if (toWrap == null) {
            return null;
        }

        return new WrappedIChunkAccess(plugin, world.getWorld(), toWrap);
    }

    @Nullable
    @Override
    public IBlockAccess c(int i, int i1) {
        ChunkCoords chunkCoords = new ChunkCoords(i, i1);
        WrappedIChunkAccess result;

        synchronized (proxies) {
            result = proxies.get(chunkCoords);

            if (result == null) {
                proxies.put(chunkCoords, result = createProxy(chunkCoords));
            }
        }

        return result;
    }

    @Override
    public void a(EnumSkyBlock var0, SectionPosition var1) {
        getWrapped().a(var0, var1);
    }

    @Override
    public IBlockAccess getWorld() {
        return getWrapped().getWorld();
    }

    private ILightAccess getWrapped() {
        // Normally, WorldServer.getChunkProvider gets passed as the ILightAccess parameter to the constructor of LightEngineThreaded
        return world.getChunkProvider();
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        org.bukkit.World world = e.getWorld();

        if (!world.getUID().equals(this.world.getWorld().getUID())) {
            return;
        }

        org.bukkit.Chunk chunk = e.getChunk();

        WrappedIChunkAccess proxy = proxies.remove(new ChunkCoords(chunk.getX(), chunk.getZ()));

        if (proxy != null) {
            proxy.unloaded();
            // System.out.printf("Removed Proxy for Chunk [%d, %d] in world %s%n", chunk.getX(), chunk.getZ(), world.getName());
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        proxies.clear();
        HandlerList.unregisterAll(this);
    }
}