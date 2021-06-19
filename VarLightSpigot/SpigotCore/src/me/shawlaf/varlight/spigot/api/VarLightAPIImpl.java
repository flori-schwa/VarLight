package me.shawlaf.varlight.spigot.api;

import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.VarLightConfig;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.AbstractBukkitExecutor;
import me.shawlaf.varlight.spigot.async.BukkitAsyncExecutorService;
import me.shawlaf.varlight.spigot.async.BukkitSyncExecutorService;
import me.shawlaf.varlight.spigot.event.CustomLuminanceUpdateEvent;
import me.shawlaf.varlight.spigot.exceptions.LightUpdateFailedException;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import me.shawlaf.varlight.spigot.persistence.Autosave;
import me.shawlaf.varlight.spigot.persistence.WorldLightPersistence;
import me.shawlaf.varlight.spigot.prompt.ChatPrompts;
import me.shawlaf.varlight.spigot.stepsize.StepsizeHandler;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

// TODO document
@ExtensionMethod({
        Objects.class,
        IntPositionExtension.class
})
public class VarLightAPIImpl implements IVarLightAPI {

    public static IVarLightAPI getAPI() {
        Plugin varLightPlugin = Bukkit.getPluginManager().getPlugin("VarLight");

        if (varLightPlugin == null) {
            throw new IllegalStateException("VarLight not present");
        }

        return ((VarLightPlugin) varLightPlugin).getApi();
    }

    private final VarLightPlugin plugin;
    private final Map<UUID, WorldLightPersistence> persistenceManagers = new HashMap<>();

    private final Map<Class<? extends IPluginLifeCycleOperations>, IPluginLifeCycleOperations> modules = new HashMap<>();

    private final AbstractBukkitExecutor syncExecutor;
    private final AbstractBukkitExecutor asyncExecutor;
    private final Autosave autosaveHandler;
    private final ChatPrompts chatPromptManager;
    private final StepsizeHandler stepsizeManager;
    private Material lightUpdateItem;

    public VarLightAPIImpl(VarLightPlugin plugin) {
        this.plugin = plugin;

        this.syncExecutor = new BukkitSyncExecutorService(plugin);
        this.asyncExecutor = new BukkitAsyncExecutorService(plugin);

        addModule(this.autosaveHandler = new Autosave(plugin));
        addModule(this.chatPromptManager = new ChatPrompts(plugin));
        addModule(this.stepsizeManager = new StepsizeHandler(plugin));

        loadLightUpdateItem();

        modules.values().forEach(IPluginLifeCycleOperations::onLoad);
    }

    public void onEnable() {
        modules.values().forEach(IPluginLifeCycleOperations::onEnable);
    }

    public void onDisable() {
        modules.values().forEach(IPluginLifeCycleOperations::onDisable);
    }

    // region Impl

    // region Getter

    @Override
    public AbstractBukkitExecutor getSyncExecutor() {
        return syncExecutor;
    }

    @Override
    public AbstractBukkitExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    @Override
    public Autosave getAutosaveHandler() {
        return autosaveHandler;
    }

    @Override
    public ChatPrompts getChatPromptManager() {
        return chatPromptManager;
    }

    @Override
    public StepsizeHandler getStepsizeManager() {
        return stepsizeManager;
    }

    @Override
    public Material getLightUpdateItem() {
        return lightUpdateItem;
    }

    // endregion

    @Override
    public @NotNull WorldLightPersistence requireVarLightEnabled(@NotNull World world) throws VarLightNotActiveException {
        world.requireNonNull("World may not be null");

        WorldLightPersistence wlp = persistenceManagers.get(world.getUID());

        if (wlp == null) {
            if (plugin.getVarLightConfig().getVarLightEnabledWorldNames().contains(world.getName())) {
                wlp = new WorldLightPersistence(world, plugin);
                persistenceManagers.put(world.getUID(), wlp);
            } else {
                throw new VarLightNotActiveException(world);
            }
        }

        return wlp;
    }

    @Override
    @NotNull
    public Collection<WorldLightPersistence> getAllActiveVarLightWorlds() {
        return persistenceManagers.values();
    }

    @Override
    public int getCustomLuminance(World world, IntPosition position) {
        world.requireNonNull("World may not be null");
        position.requireNonNull("Position may not be null");

        try {
            return requireVarLightEnabled(world).getCustomLuminance(position, 0);
        } catch (VarLightNotActiveException exception) {
            return 0;
        }
    }

    @NotNull
    public CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull World world, @NotNull IntPosition position, int customLuminance, boolean update) {
        world.requireNonNull("World may not be null");
        position.requireNonNull("Position may not be null");

        Block block = position.toBlock(world);

        int fromLight = block.getLightFromBlocks();

        if (customLuminance < 0) {
            return completedFuture(LightUpdateResult.zeroReached(fromLight, customLuminance));
        }

        if (customLuminance > 15) {
            return completedFuture(LightUpdateResult.fifteenReached(fromLight, customLuminance));
        }

        if (plugin.getNmsAdapter().isIllegalBlock(block)) {
            return completedFuture(LightUpdateResult.invalidBlock(fromLight, customLuminance));
        }

        WorldLightPersistence wlp;

        try {
            // noinspection ConstantConditions NotNull for world Already checked at the beginning of the Method
            wlp = requireVarLightEnabled(world);
        } catch (VarLightNotActiveException e) {
            return completedFuture(LightUpdateResult.notActive(fromLight, customLuminance, e));
        }

        int finalFromLight = wlp.getCustomLuminance(position, 0);

        final CustomLuminanceUpdateEvent updateEvent = new CustomLuminanceUpdateEvent(block, finalFromLight, customLuminance);
        Bukkit.getPluginManager().callEvent(updateEvent);

        if (updateEvent.isCancelled()) {
            return completedFuture(LightUpdateResult.cancelled(updateEvent.getFromLight(), updateEvent.getToLight()));
        }

        wlp.setCustomLuminance(position, updateEvent.getToLight());

        return asyncExecutor.submit(() -> {
            try {
                if (update) {
                    plugin.getLightUpdater().updateLightServer(world, position).join();
                    plugin.getLightUpdater().updateLightClient(world, position.toChunkCoords());
                }

                return LightUpdateResult.updated(finalFromLight, updateEvent.getToLight());
            } catch (VarLightNotActiveException exception) {
                throw new LightUpdateFailedException(exception);
            }
        });
    }

    @Override
    public void setCustomLuminance(@Nullable CommandSender source, @NotNull World world, @NotNull IntPosition position, int customLuminance) {
        setCustomLuminance(world, position, customLuminance, true).thenAccept(result -> {
            if (source != null) {
                result.displayMessage(source);
            }
        });
    }

    @Override
    public void setLightUpdateItem(Material item) {
        if (item == null) {
            item = Material.GLOWSTONE_DUST;
        }

        plugin.getConfig().set(VarLightConfig.CONFIG_KEY_VARLIGHT_ITEM, item.getKey().toString());
        plugin.saveConfig();

        loadLightUpdateItem();
    }

    // endregion

    // region Internals

    private <M extends IPluginLifeCycleOperations> void addModule(M module) {
        this.modules.put(module.getClass(), module);
    }

    private void loadLightUpdateItem() {
        this.lightUpdateItem = plugin.getVarLightConfig().loadLightUpdateItem();
        plugin.getLogger().info(String.format("Using \"%s\" as the Light Update Item", lightUpdateItem.getKey()));
    }

    // endregion
}
