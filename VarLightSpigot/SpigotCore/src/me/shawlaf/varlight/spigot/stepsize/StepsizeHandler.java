package me.shawlaf.varlight.spigot.stepsize;

import lombok.SneakyThrows;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class StepsizeHandler {

    private final File stepSizeYamlConfigFile;
    private final FileConfiguration stepSizeYamlConfiguration;

    public StepsizeHandler(VarLightPlugin plugin) {
        this.stepSizeYamlConfigFile = new File(plugin.getDataFolder(), "stepsizes.yml");
        this.stepSizeYamlConfiguration = YamlConfiguration.loadConfiguration(this.stepSizeYamlConfigFile);
    }

    @SneakyThrows
    public void save() {
        stepSizeYamlConfiguration.save(this.stepSizeYamlConfigFile);
    }

    public int getStepSize(Player player) {
        return stepSizeYamlConfiguration.getInt(player.getUniqueId().toString(), 1);
    }

    public void setStepSize(Player player, int stepsize) {
        stepSizeYamlConfiguration.set(player.getUniqueId().toString(), stepsize);
    }
}
