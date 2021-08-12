package me.shawlaf.varlight.spigot.api;

import me.shawlaf.varlight.spigot.async.AbstractBukkitExecutor;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.Autosave;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.spigot.prompt.ChatPrompts;
import me.shawlaf.varlight.spigot.stepsize.StepsizeHandler;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

// TODO document
public interface IVarLightAPI {

    AbstractBukkitExecutor getSyncExecutor();

    AbstractBukkitExecutor getAsyncExecutor();

    Autosave getAutosaveHandler();

    ChatPrompts getChatPromptManager();

    StepsizeHandler getStepsizeManager();

    Material getLightUpdateItem();

    @Nullable ICustomLightStorage getLightStorage(World world);

    default @NotNull ICustomLightStorage requireVarLightEnabled(World world) throws VarLightNotActiveException {
        ICustomLightStorage cls = getLightStorage(world);

        if (cls == null) {
            throw new VarLightNotActiveException(world);
        }

        return cls;
    }

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
}
