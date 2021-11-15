package me.shawlaf.varlight.spigot;

import lombok.Getter;
import me.shawlaf.varlight.spigot.api.IVarLightAPI;
import me.shawlaf.varlight.spigot.api.VarLightAPIImpl;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.exceptions.VarLightInitializationException;
import me.shawlaf.varlight.spigot.nms.IMinecraftLightUpdater;
import me.shawlaf.varlight.spigot.nms.INmsMethods;
import me.shawlaf.varlight.spigot.permissions.tree.VarLightPermissionTree;
import me.shawlaf.varlight.util.MessageUtil;
import me.shawlaf.varlight.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class VarLightPlugin extends JavaPlugin {

    /*
        TODO
         - Check for WorldGuard and WorldEdit
         - Maybe Version Check?
     */

    private static final String SERVER_VERSION;

    @Getter
    private IVarLightAPI api;
    @Getter
    private IMinecraftLightUpdater lightUpdater;
    @Getter
    private INmsMethods nmsAdapter;
    @Getter
    private VarLightConfig varLightConfig;
    @Getter
    private VarLightCommand command;

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

        this.varLightConfig = new VarLightConfig(this);
        this.api = new VarLightAPIImpl(this);

        try {
            this.nmsAdapter.onLoad();
            this.lightUpdater.onLoad();
        } catch (Exception e) {
            startUpError(e.getMessage());

            throw new VarLightInitializationException(e);
        }

        VarLightPermissionTree.init();
    }

    @Override
    public void onEnable() {
        if (!doLoad) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        ((VarLightAPIImpl) this.api).onEnable();
        this.command = new VarLightCommand(this);

        Bukkit.getPluginManager().registerEvents(new VarLightEventHandlers(this), this);

        try {
            this.nmsAdapter.onEnable();
            this.lightUpdater.onEnable();
        } catch (Exception e) {
            Bukkit.getPluginManager().disablePlugin(this);

            throw new VarLightInitializationException(e);
        }
    }

    @Override
    public void onDisable() {
        try {
            this.nmsAdapter.onDisable();
            this.lightUpdater.onDisable();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ((VarLightAPIImpl) this.api).onDisable();
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

    public void reload() {
        // TODO implement
    }

    // endregion
}
