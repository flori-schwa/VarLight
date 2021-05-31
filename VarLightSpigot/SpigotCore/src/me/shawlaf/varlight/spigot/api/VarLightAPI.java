package me.shawlaf.varlight.spigot.api;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.exception.LightUpdateFailedException;
import me.shawlaf.varlight.spigot.VarLightConfig;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.AbstractBukkitExecutor;
import me.shawlaf.varlight.spigot.async.BukkitAsyncExecutorService;
import me.shawlaf.varlight.spigot.async.BukkitSyncExecutorService;
import me.shawlaf.varlight.spigot.event.CustomLuminanceUpdateEvent;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.Autosave;
import me.shawlaf.varlight.spigot.persistence.WorldLightPersistence;
import me.shawlaf.varlight.spigot.prompt.ChatPrompts;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

// TODO document
@ExtensionMethod({
        Objects.class,
        IntPositionExtension.class
})
public class VarLightAPI {

    public static VarLightAPI getAPI() {
        Plugin varLightPlugin = Bukkit.getPluginManager().getPlugin("VarLight");

        if (varLightPlugin == null) {
            throw new IllegalStateException("VarLight not present");
        }

        return ((VarLightPlugin) varLightPlugin).getApi();
    }

    private final VarLightPlugin plugin;
    private final Map<UUID, WorldLightPersistence> persistenceManagers = new HashMap<>();

    @Getter
    private final AbstractBukkitExecutor syncExecutor;

    @Getter
    private final AbstractBukkitExecutor asyncExecutor;

    @Getter
    private final Autosave autosaveHandler;

    @Getter
    private final ChatPrompts chatPromptManager;

    @Getter
    private Material lightUpdateItem;

    public VarLightAPI(VarLightPlugin plugin) {
        this.plugin = plugin;

        this.syncExecutor = new BukkitSyncExecutorService(plugin);
        this.asyncExecutor = new BukkitAsyncExecutorService(plugin);
        this.autosaveHandler = new Autosave(plugin);
        this.chatPromptManager = new ChatPrompts(plugin);

        loadLightUpdateItem();
    }

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

    public int getCustomLuminance(@NotNull Location location) {
        location.requireNonNull("Location may not be null");
        location.getWorld().requireNonNull("Location must have an associated world");

        try {
            return requireVarLightEnabled(location.getWorld()).getCustomLuminance(location.toIntPosition(), 0);
        } catch (VarLightNotActiveException exception) {
            return 0;
        }
    }

    public void setCustomLuminance(@Nullable CommandSender source, @NotNull Location location, int customLuminance) {
        setCustomLuminance(location, customLuminance).thenAccept((result) -> {
            if (source != null) {
                result.displayMessage(source);
            }
        });
    }

    public void setLightUpdateItem(Material item) {
        if (item == null) {
            item = Material.GLOWSTONE_DUST;
        }

        plugin.getConfig().set(VarLightConfig.CONFIG_KEY_VARLIGHT_ITEM, item.getKey().toString());
        plugin.saveConfig();

        loadLightUpdateItem();
    }

    @NotNull
    public CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull Location location, int customLuminance) {
        location.requireNonNull("Location may not be null");
        location.getWorld().requireNonNull("Location must have an associated world");

        int fromLight = location.getBlock().getLightFromBlocks();

        if (customLuminance < 0) {
            return completedFuture(LightUpdateResult.zeroReached(fromLight, customLuminance));
        }

        if (customLuminance > 15) {
            return completedFuture(LightUpdateResult.fifteenReached(fromLight, customLuminance));
        }

        if (plugin.getNmsAdapter().isIllegalBlock(location.getBlock())) {
            return completedFuture(LightUpdateResult.invalidBlock(fromLight, customLuminance));
        }

        IntPosition position = location.toIntPosition();
        World world = location.getWorld();

        WorldLightPersistence wlp;

        try {
            // noinspection ConstantConditions NotNull for world Already checked at the beginning of the Method
            wlp = requireVarLightEnabled(world);
        } catch (VarLightNotActiveException e) {
            return completedFuture(LightUpdateResult.notActive(fromLight, customLuminance, e));
        }

        int finalFromLight = wlp.getCustomLuminance(position, 0);

        final CustomLuminanceUpdateEvent updateEvent = new CustomLuminanceUpdateEvent(location.getBlock(), finalFromLight, customLuminance);
        Bukkit.getPluginManager().callEvent(updateEvent);

        if (updateEvent.isCancelled()) {
            return completedFuture(LightUpdateResult.cancelled(updateEvent.getFromLight(), updateEvent.getToLight()));
        }

        wlp.setCustomLuminance(position, updateEvent.getToLight());

        return asyncExecutor.submit(() -> {
            try {
                plugin.getLightUpdater().updateLightServer(world, position).join();
                plugin.getLightUpdater().updateLightClient(world, position.toChunkCoords());

                return LightUpdateResult.updated(finalFromLight, updateEvent.getToLight());
            } catch (VarLightNotActiveException exception) {
                throw new LightUpdateFailedException(exception);
            }
        });
    }

    // region Internals

    private void loadLightUpdateItem() {
        this.lightUpdateItem = plugin.getVarLightConfig().loadLightUpdateItem();
        plugin.getLogger().info(String.format("Using \"%s\" as the Light Update Item", lightUpdateItem.getKey()));
    }

    // endregion
}
