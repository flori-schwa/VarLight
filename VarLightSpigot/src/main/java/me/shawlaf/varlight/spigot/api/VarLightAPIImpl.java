package me.shawlaf.varlight.spigot.api;

import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.VarLightConfig;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.AbstractBukkitExecutor;
import me.shawlaf.varlight.spigot.async.BukkitAsyncExecutorService;
import me.shawlaf.varlight.spigot.async.BukkitSyncExecutorService;
import me.shawlaf.varlight.spigot.bulk.BulkClearTask;
import me.shawlaf.varlight.spigot.bulk.BulkFillTask;
import me.shawlaf.varlight.spigot.bulk.BulkTaskResult;
import me.shawlaf.varlight.spigot.event.CustomLuminanceUpdateEvent;
import me.shawlaf.varlight.spigot.event.LightUpdateCause;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.module.APIModule;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import me.shawlaf.varlight.spigot.persistence.Autosave;
import me.shawlaf.varlight.spigot.persistence.CustomLightStorageNLS;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.spigot.prompt.ChatPrompts;
import me.shawlaf.varlight.spigot.stepsize.StepsizeHandler;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.completedFuture;

@ExtensionMethod({
        Objects.class,
        IntPositionExtension.class
})
public class VarLightAPIImpl implements IVarLightAPI, IVarLightAPI.Internal {

    private final VarLightPlugin plugin;
    private final Map<UUID, ICustomLightStorage> persistenceManagers = new HashMap<>();

    private final Map<Class<? extends IPluginLifeCycleOperations>, IPluginLifeCycleOperations> modules = new HashMap<>();

    private final AbstractBukkitExecutor syncExecutor;
    private final AbstractBukkitExecutor asyncExecutor;

    @APIModule
    private Autosave autosaveHandler;
    @APIModule
    private ChatPrompts chatPromptManager;
    @APIModule
    private StepsizeHandler stepsizeManager;

    private Material lightUpdateItem;

    @SneakyThrows
    public VarLightAPIImpl(VarLightPlugin plugin) {
        this.plugin = plugin;

        this.syncExecutor = new BukkitSyncExecutorService(plugin);
        this.asyncExecutor = new BukkitAsyncExecutorService(plugin);

        for (Field field : this.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(APIModule.class)) {
                continue;
            }

            if (!IPluginLifeCycleOperations.class.isAssignableFrom(field.getType())) {
                throw new IllegalStateException("Fields annotated with @" + APIModule.class.getName() + " must implement " + IPluginLifeCycleOperations.class.getName());
            }
            
            Constructor<?> constructor = field.getType().getConstructor(VarLightPlugin.class);
            Object module = constructor.newInstance(plugin);

            field.set(this, module);
            addModule(((IPluginLifeCycleOperations) module));
        }

        loadLightUpdateItem();
    }

    public void onLoad() {
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

    @Override
    public Internal unsafe() {
        return this;
    }

    // endregion

    @Override
    public @Nullable ICustomLightStorage getLightStorage(World world) {
        world.requireNonNull("World may not be null");

        ICustomLightStorage cls = persistenceManagers.get(world.getUID());

        if (cls == null) {
            if (plugin.getVarLightConfig().getVarLightEnabledWorldNames().contains(world.getName())) {
                cls = new CustomLightStorageNLS(world, plugin);
                persistenceManagers.put(world.getUID(), cls);
            }
        }

        return cls;
    }

    @Override
    @NotNull
    public Collection<ICustomLightStorage> getAllActiveVarLightWorlds() {
        return persistenceManagers.values();
    }

    @Override
    public int getCustomLuminance(World world, IntPosition position) {
        world.requireNonNull("World may not be null");
        position.requireNonNull("Position may not be null");

        try {
            return requireVarLightEnabled(world).getCustomLuminance(position);
        } catch (VarLightNotActiveException exception) {
            return 0;
        }
    }

    @NotNull
    public CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull World world, @NotNull IntPosition position, int customLuminance, boolean update, LightUpdateCause cause) {
        if (!Bukkit.isPrimaryThread()) {
            return syncExecutor.submit(() -> setCustomLuminance(world, position, customLuminance, update)).join();
        }

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

        ICustomLightStorage wlp;

        try {
            // noinspection ConstantConditions NotNull for world Already checked at the beginning of the Method
            wlp = requireVarLightEnabled(world);
        } catch (VarLightNotActiveException e) {
            return completedFuture(LightUpdateResult.notActive(fromLight, customLuminance, e));
        }

        int finalFromLight = wlp.getCustomLuminance(position);

        final CustomLuminanceUpdateEvent updateEvent = new CustomLuminanceUpdateEvent(block, finalFromLight, customLuminance, cause);

        Bukkit.getPluginManager().callEvent(updateEvent);

        if (updateEvent.isCancelled()) {
            return completedFuture(LightUpdateResult.cancelled(updateEvent.getFromLight(), updateEvent.getToLight()));
        }

        wlp.setCustomLuminance(position, updateEvent.getToLight());

        LightUpdateResult result = LightUpdateResult.updated(finalFromLight, updateEvent.getToLight());

        if (update) {
            return asyncExecutor.submit(() -> {
                plugin.getLightUpdater().updateLightSingleBlock(wlp, position).join();

                return result;
            });
        }

        return CompletableFuture.completedFuture(result);
    }

    private void ensureMainThread(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            syncExecutor.submit(runnable);
        }
    }

    @Override
    public void setCustomLuminance(@Nullable CommandSender source, LightUpdateCause.Type causeType, @NotNull World world, @NotNull IntPosition position, int customLuminance) {

        LightUpdateCause cause;

        switch (causeType) {
            case PLAYER: {
                cause = LightUpdateCause.player(source);
                break;
            }

            case COMMAND: {
                cause = LightUpdateCause.player(source);
            }

            case API: {
                cause = LightUpdateCause.api();
            }

            default: {
                throw new IllegalStateException("Default block reached");
            }
        }

        setCustomLuminance(world, position, customLuminance, true, cause).thenAccept(result -> {
            if (source != null) {
                result.displayMessage(source);
            }
        });
    }

    @Override
    public CompletableFuture<BulkTaskResult> runBulkClear(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end) {
        return new BulkClearTask(plugin, world, source, start, end).run();
    }

    @Override
    public CompletableFuture<BulkTaskResult> runBulkFill(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end, int lightLevel, @Nullable Predicate<Block> filter) {
        return new BulkFillTask(plugin, world, source, start, end, lightLevel, filter).run();
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

    public <M extends IPluginLifeCycleOperations> void addModule(M module) {
        this.modules.put(module.getClass(), module);
    }

    private void loadLightUpdateItem() {
        this.lightUpdateItem = plugin.getVarLightConfig().loadLightUpdateItem();
        plugin.getLogger().info(String.format("Using \"%s\" as the Light Update Item", lightUpdateItem.getKey()));
    }

    // endregion
}
