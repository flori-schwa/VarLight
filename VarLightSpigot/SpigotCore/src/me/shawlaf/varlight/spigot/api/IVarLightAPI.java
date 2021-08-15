package me.shawlaf.varlight.spigot.api;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.AbstractBukkitExecutor;
import me.shawlaf.varlight.spigot.bulk.BulkTaskResult;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.Autosave;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.spigot.prompt.ChatPrompts;
import me.shawlaf.varlight.spigot.stepsize.StepsizeHandler;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

// TODO document
public interface IVarLightAPI {

    static IVarLightAPI getAPI() {
        Plugin varLightPlugin = Bukkit.getPluginManager().getPlugin("VarLight");

        if (varLightPlugin == null) {
            throw new IllegalStateException("VarLight not present");
        }

        return ((VarLightPlugin) varLightPlugin).getApi();
    }

    AbstractBukkitExecutor getSyncExecutor();

    AbstractBukkitExecutor getAsyncExecutor();

    Autosave getAutosaveHandler();

    ChatPrompts getChatPromptManager();

    StepsizeHandler getStepsizeManager();

    Material getLightUpdateItem();

    /**
     * <p>
     *  Internal API
     * </p>
     *
     * <p>
     *  The Light Update Methods found in {@link ICustomLightStorage} only store the Custom Light Level in persistence. <strong>No Light Updates will be performed on the Server or Client</strong>
     * </p>
     */
    Internal unsafe();

    @NotNull Collection<ICustomLightStorage> getAllActiveVarLightWorlds();

    void setLightUpdateItem(Material item);

    int getCustomLuminance(World world, IntPosition position);

    default int getCustomLuminance(@NotNull Location location) {
        return getCustomLuminance(location.getWorld(), IntPositionExtension.toIntPosition(location));
    }

    CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull World world, @NotNull IntPosition position, int customLuminance, boolean update);

    default CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull World world, @NotNull IntPosition position, int customLuminance) {
        return setCustomLuminance(world, position, customLuminance, true);
    }

    default CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull Location location, int customLuminance, boolean update) {
        return setCustomLuminance(location.getWorld(), IntPositionExtension.toIntPosition(location), customLuminance, update);
    }

    default CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull Location location, int customLuminance) {
        return setCustomLuminance(location, customLuminance, true);
    }

    void setCustomLuminance(@Nullable CommandSender source, @NotNull World world, @NotNull IntPosition position, int customLuminance);

    default void setCustomLuminance(@Nullable CommandSender source, @NotNull Location location, int customLuminance) {
        setCustomLuminance(source, location.getWorld(), IntPositionExtension.toIntPosition(location), customLuminance);
    }

    default CompletableFuture<LightUpdateResult> clearCustomLuminance(@NotNull World world, @NotNull IntPosition position, boolean update) {
        return setCustomLuminance(world, position, 0, update);
    }

    default CompletableFuture<LightUpdateResult> clearCustomLuminance(@NotNull World world, IntPosition position) {
        return clearCustomLuminance(world, position, true);
    }

    default CompletableFuture<LightUpdateResult> clearCustomLuminance(@NotNull Location location, boolean update) {
        return clearCustomLuminance(location.getWorld(), IntPositionExtension.toIntPosition(location), update);
    }

    default CompletableFuture<LightUpdateResult> clearCustomLuminance(@NotNull Location location) {
        return clearCustomLuminance(location, true);
    }

    CompletableFuture<BulkTaskResult> runBulkClear(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end);

    CompletableFuture<BulkTaskResult> runBulkFill(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end, int lightLevel, @Nullable Predicate<Material> filter);

    default CompletableFuture<BulkTaskResult> runBulkFill(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end, int lightLevel) {
        return runBulkFill(world, source, start, end, lightLevel, x -> true);
    }

    /**
     * <p>
     *  Internal API
     * </p>
     *
     * <p>
     *  The Light Update Methods found in {@link ICustomLightStorage} only store the Custom Light Level in persistence. <strong>No Light Updates will be performed on the Server or Client</strong>
     * </p>
     */
    interface Internal {
        @Nullable ICustomLightStorage getLightStorage(World world);

        default ICustomLightStorage requireVarLightEnabled(World world) throws VarLightNotActiveException {
            ICustomLightStorage cls = getLightStorage(world);

            if (cls == null) {
                throw new VarLightNotActiveException(world);
            }

            return cls;
        }
    }
}
