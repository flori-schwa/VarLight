package me.shawlaf.varlight.spigot;

import lombok.Getter;
import me.shawlaf.varlight.spigot.api.VarLightAPI;
import me.shawlaf.varlight.spigot.exceptions.VarLightInitializationException;
import me.shawlaf.varlight.spigot.nms.IMinecraftLightUpdater;
import me.shawlaf.varlight.spigot.nms.INmsMethods;
import me.shawlaf.varlight.spigot.util.MessageUtil;
import me.shawlaf.varlight.spigot.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class VarLightPlugin extends JavaPlugin {

    /*
        TODO
         - Autosave
         - Light Update Item
         - Check for WorldGuard and WorldEdit
         - Commands
         - Maybe Version Check?
         - Persistence
         - Player Modify Light Source Events
     */

    private static final String SERVER_VERSION;

    @Getter
    private VarLightAPI api;
    @Getter
    private IMinecraftLightUpdater lightUpdater;
    @Getter
    private INmsMethods nmsAdapter;
    @Getter
    private VarLightConfig varLightConfig;

    private boolean doLoad = true;

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName();

        SERVER_VERSION = version.substring(version.lastIndexOf('.') + 1);
    }

    {
        try {
            Class<?> nmsLightUpdaterClass = Class.forName(String.format("me.shawlaf.varlight.spigot.nms.%s.LightUpdater", SERVER_VERSION));
            Class<?> nmsAdapterClass = Class.forName(String.format("me.shawlaf.varlight.spigot.nms.%s.NmsAdapter", SERVER_VERSION));

            this.lightUpdater = (IMinecraftLightUpdater) nmsLightUpdaterClass.getConstructor(VarLightPlugin.class).newInstance(this);
            this.nmsAdapter = (INmsMethods) nmsAdapterClass.getConstructor(VarLightPlugin.class).newInstance(this);
        } catch (ClassNotFoundException e) {
            String errMsg = String.format("No VarLight implementation present for Minecraft Version %s (%s): Could not find Class %s", Bukkit.getVersion(), SERVER_VERSION, e.getMessage());
            startUpError(errMsg);

            throw new VarLightInitializationException(errMsg, e);
        } catch (Throwable e) {
            String errMsg = String.format("Failed to initialize VarLight for Minecraft Version %s (%s): %s", Bukkit.getVersion(), SERVER_VERSION, e.getMessage());
            startUpError(errMsg);

            throw new VarLightInitializationException(errMsg, e);
        }
    }

    @Override
    public void onLoad() {
        if (!doLoad) {
            return;
        }

        this.api = new VarLightAPI(this);
        this.varLightConfig = new VarLightConfig(this);
    }

    @Override
    public void onEnable() {
        if (!doLoad) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
    }

    // region Util

    private void startUpError(String message) {
        final String sep = StringUtil.repeat("-", 80);
        final String[] msg = MessageUtil.splitLongMessage(message, sep.length());

        getLogger().severe(sep);

        for (String s : msg) {
            getLogger().severe(s);
        }

        getLogger().severe(sep);

        doLoad = false;
    }

    // endregion
}
