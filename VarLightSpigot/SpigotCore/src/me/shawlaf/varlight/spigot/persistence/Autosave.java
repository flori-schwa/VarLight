package me.shawlaf.varlight.spigot.persistence;

import lombok.Getter;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.Ticks;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class Autosave implements Listener, IPluginLifeCycleOperations {

    public Autosave(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
        for (World world : plugin.getVarLightConfig().getVarLightEnabledWorlds()) {
            try {
                plugin.getApi().requireVarLightEnabled(world).runAutosave();
            } catch (VarLightNotActiveException ignored) {

            }
        }
    }

    public enum Strategy {
        ON_WORLD_SAVE,
        DISABLED,
        TIMED
    }

    private final VarLightPlugin plugin;

    private BukkitTask autosaveTask;
    @Getter
    private Strategy strategy;

    /**
     * <p>
     * Disables automatic saving of Custom Light Sources
     * </p>
     */
    public void disable() {
        update(0);
    }

    /**
     * <p>
     * Automatically saves Custom Light Sources every {@code n} Minutes
     * </p>
     *
     * @param interval The amount of Minutes in between every automatic save
     */
    public void setTimed(int interval) {
        if (interval < 1) {
            throw new IllegalArgumentException(String.format("save interval must be >= 1, got %d", interval));
        }

        update(interval);
    }

    /**
     * <p>
     * Automatically saves Custom Light Sources when a {@link WorldSaveEvent} is called
     * </p>
     */
    public void setOnWorldSave() {
        update(-1);
    }

    @EventHandler
    private void onWorldSave(WorldSaveEvent e) {
        if (strategy != Strategy.ON_WORLD_SAVE) {
            return;
        }

        try {
            plugin.getApi().requireVarLightEnabled(e.getWorld()).runAutosave();
        } catch (VarLightNotActiveException ignored) {
            // Ignore any worlds, that are not VarLight enabled
        }
    }

    private void update(int interval) {
        if (autosaveTask != null && !autosaveTask.isCancelled()) {
            autosaveTask.cancel();
            autosaveTask = null;
        }

        if (interval < 0) {
            plugin.getLogger().info("Light sources are automatically saved on world save");
            strategy = Strategy.ON_WORLD_SAVE;

            return;
        }

        if (interval == 0) {
            plugin.getLogger().warning("Light sources not saved automatically! You must run /varlight save manually to save Light sources!");
            strategy = Strategy.DISABLED;

            return;
        }

        plugin.getLogger().info(String.format("Light sources automatically saved every %d Minutes", interval));
        strategy = Strategy.TIMED;

        final long ticks = Ticks.calculate(interval, TimeUnit.MINUTES);

        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World world : plugin.getVarLightConfig().getVarLightEnabledWorlds()) {
                try {
                    plugin.getApi().requireVarLightEnabled(world).runAutosave();
                } catch (VarLightNotActiveException notPossible) {
                    notPossible.printStackTrace();
                }
            }
        }, ticks, ticks);
    }
}
