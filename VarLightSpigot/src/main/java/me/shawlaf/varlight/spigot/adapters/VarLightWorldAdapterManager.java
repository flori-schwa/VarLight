package me.shawlaf.varlight.spigot.adapters;

import me.shawlaf.varlight.adapter.IWorld;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VarLightWorldAdapterManager implements Listener, IPluginLifeCycleOperations {

    private final VarLightPlugin _plugin;
    private final Map<UUID, IWorld> _adapters = new HashMap<>();

    public VarLightWorldAdapterManager(VarLightPlugin plugin) {
        _plugin = plugin;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, _plugin);
    }

    @Override
    public void onDisable() {
        _adapters.clear();
    }

    public IWorld adapt(World world) {
        return _adapters.computeIfAbsent(world.getUID(), key -> new VarLightWorld(world));
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        _adapters.remove(event.getWorld().getUID());
    }

    private record VarLightWorld(World bukkitWorld) implements IWorld {
        @Override
        public int getMinHeight() {
            return bukkitWorld.getMinHeight();
        }

        @Override
        public int getMaxHeight() {
            return bukkitWorld.getMaxHeight();
        }
    }


}
