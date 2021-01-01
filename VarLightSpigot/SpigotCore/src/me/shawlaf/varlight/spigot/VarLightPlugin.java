package me.shawlaf.varlight.spigot;

import lombok.Getter;
import me.shawlaf.varlight.spigot.api.VarLightAPI;
import me.shawlaf.varlight.spigot.nms.IMinecraftLightUpdater;
import me.shawlaf.varlight.spigot.nms.INmsMethods;
import org.bukkit.plugin.java.JavaPlugin;

public class VarLightPlugin extends JavaPlugin {

    @Getter
    private final VarLightAPI api = new VarLightAPI(this);
    @Getter private IMinecraftLightUpdater lightUpdater;
    @Getter private INmsMethods nmsAdapter;
    @Getter private VarLightConfig varLightConfig;

}
