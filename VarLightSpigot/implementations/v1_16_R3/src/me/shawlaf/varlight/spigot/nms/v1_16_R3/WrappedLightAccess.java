package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
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

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WrappedLightAccess implements ILightAccess, Listener {

    // TODO MEMORY LEAK, The created Proxies are not Garbage collected

    private final Map<ChunkCoords, IChunkAccess> proxies = Collections.synchronizedMap(new HashMap<>());

    private final VarLightPlugin plugin;
    private final WorldServer world;

    public WrappedLightAccess(VarLightPlugin plugin, WorldServer world) {
        this.plugin = plugin;
        this.world = world;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private int getCustomLuminance(IChunkAccess blockAccess, BlockPosition bPos) {
        int vanilla = blockAccess.getType(bPos).f();

        try {
            ICustomLightStorage wlp = plugin.getApi().unsafe().requireVarLightEnabled(world.getWorld());

            return wlp.getCustomLuminance(new IntPosition(bPos.getX(), bPos.getY(), bPos.getZ()), vanilla);
        } catch (VarLightNotActiveException e) {
            return vanilla;
        }
    }

    private IChunkAccess createProxy(ChunkCoords chunkCoords) {
        IChunkAccess toWrap = (IChunkAccess) getWrapped().c(chunkCoords.x, chunkCoords.z);

        if (toWrap == null) {
            return null;
        }

        // TODO use proper debug logging
//        System.out.printf("Created new IChunkAccess Proxy for Chunk %s in world %s%n", chunkCoords.toShortString(), world.getWorld().getName());

        return (IChunkAccess) Proxy.newProxyInstance(
                IChunkAccess.class.getClassLoader(),
                new Class[]{IChunkAccess.class},

                (proxy, method, args) -> {
                    if (method.getName().equals("g")) {
                        return getCustomLuminance(toWrap, (BlockPosition) args[0]);
                    }

                    if (method.getName().equals("finalize")) {
//                        System.out.printf("Deleted IChunkAccess Proxy for Chunk %s in world %s%n", chunkCoords.toShortString(), world.getWorld().getName());
                    }

                    return method.invoke(toWrap, args);
                }
        );
    }

    @Nullable
    @Override
    public IBlockAccess c(int i, int i1) {
        ChunkCoords chunkCoords = new ChunkCoords(i, i1);
        IChunkAccess result;

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

        if (proxies.remove(new ChunkCoords(chunk.getX(), chunk.getZ())) != null) {
//            System.out.printf("Removed Proxy for Chunk [%d, %d] in world %s%n", chunk.getX(), chunk.getZ(), world.getName());
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        proxies.clear();
        HandlerList.unregisterAll(this);
    }
}