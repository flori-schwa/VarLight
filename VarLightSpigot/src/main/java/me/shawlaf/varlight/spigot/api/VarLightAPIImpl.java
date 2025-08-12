package me.shawlaf.varlight.spigot.api;


import me.shawlaf.varlight.adapter.IWorld;
import me.shawlaf.varlight.spigot.VarLightConfig;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.adapters.VarLightWorldAdapterManager;
import me.shawlaf.varlight.spigot.async.AbstractBukkitExecutor;
import me.shawlaf.varlight.spigot.async.BukkitAsyncExecutorService;
import me.shawlaf.varlight.spigot.async.BukkitSyncExecutorService;
import me.shawlaf.varlight.spigot.bulk.BulkClearTask;
import me.shawlaf.varlight.spigot.bulk.BulkFillTask;
import me.shawlaf.varlight.spigot.bulk.BulkTaskResult;
import me.shawlaf.varlight.spigot.event.CustomLuminanceUpdateEvent;
import me.shawlaf.varlight.spigot.event.LightUpdateCause;
import me.shawlaf.varlight.spigot.glowingitems.GlowItemStack;
import me.shawlaf.varlight.spigot.module.APIModule;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import me.shawlaf.varlight.spigot.persistence.Autosave;
import me.shawlaf.varlight.spigot.persistence.CustomLightStorageNLS;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.spigot.prompt.ChatPrompts;
import me.shawlaf.varlight.spigot.stepsize.StepsizeHandler;
import me.shawlaf.varlight.spigot.util.IntPositionUtil;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class VarLightAPIImpl implements IVarLightAPI, IVarLightAPI.Internal {

    private final VarLightPlugin _plugin;
    private final Map<UUID, ICustomLightStorage> _persistenceManagers = new HashMap<>();

    private final Map<Class<? extends IPluginLifeCycleOperations>, IPluginLifeCycleOperations> _modules = new HashMap<>();

    private final AbstractBukkitExecutor _syncExecutor;
    private final AbstractBukkitExecutor _asyncExecutor;

    @APIModule
    private Autosave _autosaveHandler;
    @APIModule
    private ChatPrompts _chatPromptManager;
    @APIModule
    private StepsizeHandler _stepsizeManager;
    @APIModule
    private VarLightWorldAdapterManager _worldAdapterManager;

    private Material _lightUpdateItem;

    public VarLightAPIImpl(VarLightPlugin plugin) {
        _plugin = plugin;

        _syncExecutor = new BukkitSyncExecutorService(plugin);
        _asyncExecutor = new BukkitAsyncExecutorService(plugin);

        initializeApiModules(plugin);
        _lightUpdateItem = loadLightUpdateItem(plugin);
    }

    private void initializeApiModules(VarLightPlugin plugin) {
        try {
            for (Field field : getClass().getDeclaredFields()) {
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
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void onLoad() {
        _modules.values().forEach(IPluginLifeCycleOperations::onLoad);
    }

    public void onEnable() {
        _modules.values().forEach(IPluginLifeCycleOperations::onEnable);
    }

    public void onDisable() {
        _modules.values().forEach(IPluginLifeCycleOperations::onDisable);
    }

    // region Impl

    // region Getter


    @Override
    public VarLightConfig getConfiguration() {
        return _plugin.getVarLightConfig();
    }

    @Override
    public AbstractBukkitExecutor getSyncExecutor() {
        return _syncExecutor;
    }

    @Override
    public AbstractBukkitExecutor getAsyncExecutor() {
        return _asyncExecutor;
    }

    @Override
    public Autosave getAutosaveHandler() {
        return _autosaveHandler;
    }

    @Override
    public ChatPrompts getChatPromptManager() {
        return _chatPromptManager;
    }

    @Override
    public StepsizeHandler getStepsizeManager() {
        return _stepsizeManager;
    }

    @Override
    public Material getLightUpdateItem() {
        return _lightUpdateItem;
    }

    @Override
    public Internal unsafe() {
        return this;
    }

    // endregion

    @Override
    public @Nullable ICustomLightStorage getLightStorage(World world) {
        Objects.requireNonNull(world, "World may not be null");

        ICustomLightStorage cls = _persistenceManagers.get(world.getUID());

        if (cls == null) {
            if (_plugin.getVarLightConfig().getVarLightEnabledWorldNames().contains(world.getName())) {
                cls = new CustomLightStorageNLS(world, _plugin);
                _persistenceManagers.put(world.getUID(), cls);
            }
        }

        return cls;
    }

    @Override
    @NotNull
    public Collection<ICustomLightStorage> getAllActiveVarLightWorlds() {
        return _persistenceManagers.values();
    }

    @Override
    public int getCustomLuminance(@NotNull World world, @NotNull IntPosition position) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(position);

        return Optional.ofNullable(getLightStorage(world)).map(cls -> cls.getCustomLuminance(position)).orElse(0);
    }

    @Override
    public boolean isVarLightEnabled(@NotNull World world) {
        return getLightStorage(Objects.requireNonNull(world)) != null;
    }

    @Override
    public @NotNull GlowItemStack createGlowItemStack(@NotNull ItemStack base, int lightLevel) {
        return new GlowItemStack(_plugin, Objects.requireNonNull(base), lightLevel);
    }

    @Override
    public @Nullable GlowItemStack importGlowItemStack(@NotNull ItemStack glowingStack) {
        try {
            return new GlowItemStack(_plugin, Objects.requireNonNull(glowingStack));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @NotNull
    public CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull World world, @NotNull IntPosition position, int customLuminance, boolean update, LightUpdateCause cause) {
        if (!Bukkit.isPrimaryThread()) {
            return _syncExecutor.submit(() -> setCustomLuminance(world, position, customLuminance, update, cause)).join();
        }

        Objects.requireNonNull(world, "World may not be null");
        Objects.requireNonNull(position, "Position may not be null");

        Block block = IntPositionUtil.toBlock(position, world);

        int fromLight = block.getLightFromBlocks();

        if (customLuminance < 0) {
            return completedFuture(LightUpdateResult.zeroReached(fromLight, customLuminance));
        }

        if (customLuminance > 15) {
            return completedFuture(LightUpdateResult.fifteenReached(fromLight, customLuminance));
        }

        if (_plugin.getNmsAdapter().isIllegalBlock(block)) {
            return completedFuture(LightUpdateResult.invalidBlock(fromLight, customLuminance));
        }

        ICustomLightStorage cls;

        if ((cls = getLightStorage(world)) == null) {
            return completedFuture(LightUpdateResult.notActive(fromLight, customLuminance, world));
        }

        int finalFromLight = cls.getCustomLuminance(position);

        final CustomLuminanceUpdateEvent updateEvent = new CustomLuminanceUpdateEvent(block, finalFromLight, customLuminance, cause);

        Bukkit.getPluginManager().callEvent(updateEvent);

        if (updateEvent.isCancelled()) {
            return completedFuture(LightUpdateResult.cancelled(updateEvent.getFromLight(), updateEvent.getToLight()));
        }

        cls.setCustomLuminance(position, updateEvent.getToLight());

        LightUpdateResult result = LightUpdateResult.updated(finalFromLight, updateEvent.getToLight());

        if (update) {
            return _asyncExecutor.submit(() -> {
                _plugin.getLightUpdater().updateLightSingleBlock(cls, position).join();

                return result;
            });
        }

        return CompletableFuture.completedFuture(result);
    }

    private void ensureMainThread(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            _syncExecutor.submit(runnable);
        }
    }

    @Override
    public void setCustomLuminance(@Nullable CommandSender source, LightUpdateCause.Type causeType, @NotNull World world, @NotNull IntPosition position, int customLuminance) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(source);

        final LightUpdateCause cause = switch (causeType) {
            case PLAYER ->
                    LightUpdateCause.player(source, LightUpdateCause.PlayerAction.UNSPECIFIED); // TODO expand this API
            case COMMAND -> LightUpdateCause.command(source);
            case API -> LightUpdateCause.api();
        };

        setCustomLuminance(world, position, customLuminance, true, cause) //
                .thenAccept(result -> result.displayMessage(source));
    }

    @Override
    public CompletableFuture<BulkTaskResult> runBulkClear(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end) {
        return new BulkClearTask(_plugin, Objects.requireNonNull(world), Objects.requireNonNull(source), Objects.requireNonNull(start), Objects.requireNonNull(end)).run();
    }

    @Override
    public CompletableFuture<BulkTaskResult> runBulkFill(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end, int lightLevel, @Nullable Predicate<Block> filter) {
        return new BulkFillTask(_plugin, Objects.requireNonNull(world), Objects.requireNonNull(source), Objects.requireNonNull(start), Objects.requireNonNull(end), lightLevel, filter).run();
    }

    @Override
    public void setLightUpdateItem(Material item) {
        if (item == null) {
            item = Material.GLOWSTONE_DUST;
        }

        _plugin.getConfig().set(VarLightConfig.CONFIG_KEY_VARLIGHT_ITEM, item.getKeyOrThrow().toString());
        _plugin.saveConfig();

        _lightUpdateItem = loadLightUpdateItem(_plugin);
    }

    // endregion

    // region Adapters

    @Override
    public IWorld adapt(World bukkitWorld) {
        return _worldAdapterManager.adapt(bukkitWorld);
    }

    // endregion

    // region Internals

    public <M extends IPluginLifeCycleOperations> void addModule(M module) {
        _modules.put(module.getClass(), module);
    }

    private static Material loadLightUpdateItem(VarLightPlugin plugin) {
        Material lui = plugin.getVarLightConfig().loadLightUpdateItem();
        plugin.getLogger().info(String.format("Using \"%s\" as the Light Update Item", lui.getKeyOrThrow()));
        return lui;
    }

    // endregion
}
