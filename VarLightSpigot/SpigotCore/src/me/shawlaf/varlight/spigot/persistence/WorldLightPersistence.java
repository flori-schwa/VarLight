package me.shawlaf.varlight.spigot.persistence;

import lombok.Getter;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.World;

public class WorldLightPersistence {

    @Getter
    private final World forBukkitWorld;
    private final VarLightPlugin plugin;

    public WorldLightPersistence(World forBukkitWorld, VarLightPlugin plugin) {
        this.forBukkitWorld = forBukkitWorld;
        this.plugin = plugin;
    }
}
