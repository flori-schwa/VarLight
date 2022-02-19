package me.shawlaf.varlight.spigot;

import lombok.Getter;
import me.shawlaf.varlight.persistence.migrate.LightDatabaseMigrator;
import me.shawlaf.varlight.spigot.api.IVarLightAPI;
import me.shawlaf.varlight.spigot.api.VarLightAPIImpl;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.exceptions.VarLightInitializationException;
import me.shawlaf.varlight.spigot.nms.IMinecraftLightUpdater;
import me.shawlaf.varlight.spigot.nms.INmsMethods;
import me.shawlaf.varlight.spigot.permissions.tree.VarLightPermissionTree;
import me.shawlaf.varlight.spigot.persistence.migrations.JsonToNLSMigration;
import me.shawlaf.varlight.spigot.persistence.migrations.MoveVarLightRootFolder;
import me.shawlaf.varlight.spigot.persistence.migrations.VLDBToNLSMigration;
import me.shawlaf.varlight.spigot.updatecheck.VarLightUpdateCheck;
import me.shawlaf.varlight.util.MessageUtil;
import me.shawlaf.varlight.util.NumericMajorMinorVersion;
import me.shawlaf.varlight.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class VarLightPlugin extends JavaPlugin {

    private VarLightAPIImpl api;
    @Getter
    private IMinecraftLightUpdater lightUpdater;
    @Getter
    private INmsMethods nmsAdapter;
    @Getter
    private VarLightConfig varLightConfig;
    @Getter
    private VarLightCommand command;
    @Getter
    private LightDatabaseMigrator<World> lightDatabaseMigrator;

    private boolean doLoad = true;

    {
        try {
            this.lightUpdater = new me.shawlaf.varlight.spigot.nms.v1_16_R3.LightUpdater(this);
            this.nmsAdapter = new me.shawlaf.varlight.spigot.nms.v1_16_R3.NmsAdapter(this);
        } catch (Throwable e) {
            String packageVersion = Bukkit.getServer().getClass().getPackage().getName();
            packageVersion = packageVersion.substring(packageVersion.lastIndexOf('.') + 1);

            String errMsg = String.format("Failed to initialize VarLight for Minecraft Version %s (%s): %s", Bukkit.getVersion(), packageVersion, e.getMessage());
            startUpError(errMsg);

            throw new VarLightInitializationException(errMsg, e);
        }
    }

    @Override
    public void onLoad() {
        if (!doLoad) {
            return;
        }

        this.lightDatabaseMigrator = new LightDatabaseMigrator<World>(getLogger()) {
            @Override
            protected File getVarLightSaveDirectory(World world) {
                return nmsAdapter.getVarLightSaveDirectory(world);
            }

            @Override
            protected String getName(World world) {
                return world.getName();
            }
        };

        this.lightDatabaseMigrator.addDataMigrations(new JsonToNLSMigration(this), new VLDBToNLSMigration(this));
        this.lightDatabaseMigrator.addStructureMigrations(new MoveVarLightRootFolder(this));

        this.varLightConfig = new VarLightConfig(this);
        this.api = new VarLightAPIImpl(this);

        this.api.addModule(this.nmsAdapter);
        this.api.addModule(this.lightUpdater);

        VarLightPermissionTree.init();

        try {
            this.api.onLoad();
        } catch (Exception e) {
            startUpError(e.getMessage());

            throw new VarLightInitializationException(e);
        }
    }

    @Override
    public void onEnable() {
        if (!doLoad) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.api.onEnable();
        } catch (Exception e) {
            Bukkit.getPluginManager().disablePlugin(this);

            throw new VarLightInitializationException(e);
        }

        this.command = new VarLightCommand(this);

        Bukkit.getPluginManager().registerEvents(new VarLightEventHandlers(this), this);

        if (varLightConfig.isCheckUpdateEnabled()) {
            NumericMajorMinorVersion currentVersion = NumericMajorMinorVersion.tryParse(getDescription().getVersion());

            if (currentVersion != null) {
                api.getAsyncExecutor().submit(new VarLightUpdateCheck(getLogger(), currentVersion));
            }
        }
    }

    @Override
    public void onDisable() {
        this.api.onDisable();
    }

    public IVarLightAPI getApi() {
        return this.api;
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

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        this.varLightConfig = new VarLightConfig(this);
    }

    // endregion
}
