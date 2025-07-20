package me.shawlaf.varlight.spigot.stepsize;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.Ticks;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class StepsizeHandler implements IPluginLifeCycleOperations {

    private final VarLightPlugin plugin;

    private File stepSizeYamlConfigFile;
    private FileConfiguration stepSizeYamlConfiguration;
    private boolean hasChanges = false;
    private BukkitTask saveTask;

    public StepsizeHandler(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad() {
        this.stepSizeYamlConfigFile = new File(plugin.getDataFolder(), "stepsizes.yml");
        this.stepSizeYamlConfiguration = YamlConfiguration.loadConfiguration(this.stepSizeYamlConfigFile);
    }

    @Override
    public void onEnable() {
        this.saveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (hasChanges) {
                saveConfiguration();
                hasChanges = false;
            }
        }, Ticks.calculate(5, TimeUnit.MINUTES), Ticks.calculate(5, TimeUnit.MINUTES));
    }

    @Override
    public void onDisable() {
        saveTask.cancel();
        saveConfiguration();
    }

    private void saveConfiguration() {
        try {
            stepSizeYamlConfiguration.save(this.stepSizeYamlConfigFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getStepSize(Player player) {
        return stepSizeYamlConfiguration.getInt(player.getUniqueId().toString(), 1);
    }

    public void setStepSize(Player player, int stepsize) {
        if (getStepSize(player) != stepsize) {
            hasChanges = true;
        }

        stepSizeYamlConfiguration.set(player.getUniqueId().toString(), stepsize);
    }
}
