package me.shawlaf.varlight.spigot.api;

import me.shawlaf.varlight.exception.VarLightIOException;
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
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

/**
 * <p>VarLight Developer API</p>
 */
public interface IVarLightAPI {

    /**
     * @return The Singleton API Instance
     */
    static IVarLightAPI getAPI() {
        Plugin varLightPlugin = Bukkit.getPluginManager().getPlugin("VarLight");

        if (varLightPlugin == null) {
            throw new IllegalStateException("VarLight not present");
        }

        return ((VarLightPlugin) varLightPlugin).getApi();
    }

    /**
     * @return An {@link ExecutorService} used to schedule tasks on the Server's Main Thread
     */
    AbstractBukkitExecutor getSyncExecutor();

    /**
     * @return An {@link ExecutorService} used to schedule tasks asynchronously
     */
    AbstractBukkitExecutor getAsyncExecutor();

    /**
     * @return The Autosave Handler
     * @see Autosave
     */
    Autosave getAutosaveHandler();

    /**
     * @return The Chat Prompt Manager
     * @see ChatPrompts
     */
    ChatPrompts getChatPromptManager();

    /**
     * @return The Stepsize Handler
     * @see StepsizeHandler
     */
    StepsizeHandler getStepsizeManager();

    /**
     * @return The currently used Light Update Item as specified in the plugin's configuration file
     */
    Material getLightUpdateItem();

    /**
     * <p>
     * Sets the Light Update Item to use for plugin's main functionality
     * </p>
     *
     * <p>
     * The specified value will always be written to the configuration file, however if the specified Item
     * does not meet all requirements, the default item (Glowstone Dust) will be used.
     * </p>
     *
     * <p>
     * Requirements of the Light Update Item:
     * <p>
     *         <ul>
     *             <li>Must not be {@code null}</li>
     *             <li>{@link Material#isBlock()} must be {@code false}</li>
     *             <li>{@link Material#isItem()} must be {@code true}</li>
     *         </ul>
     *     </p>
     * </p>
     *
     * @param item The {@link Material} to set as the Light Update Item
     */
    void setLightUpdateItem(Material item);

    /**
     * <p>
     * Returns the custom Light Level at the specified Position, or {@code 0} if no custom Light source exists at the given position.
     * </p>
     *
     * <p>
     * If the Region containing the Position is first queried, the data is read from disk and loaded into memory first.<br/>
     * If an {@link IOException} occurs, a {@link VarLightIOException} wrapping the thrown {@link IOException} will be thrown
     * </p>
     *
     * @param world    The {@link World} containing the custom Light Source
     * @param position The {@link IntPosition} to query for custom Light Data
     * @return The custom Light Level at the specified Location, or {@code 0} if there is no custom Light Source
     */
    int getCustomLuminance(World world, IntPosition position);

    /**
     * <p>
     * Same as {@link IVarLightAPI#getCustomLuminance(World, IntPosition)} but taking a {@link Location} as an Argument
     * </p>
     *
     * @see IVarLightAPI#getCustomLuminance(World, IntPosition)
     */
    default int getCustomLuminance(@NotNull Location location) {
        return getCustomLuminance(location.getWorld(), IntPositionExtension.toIntPosition(location));
    }

    /**
     * <p>
     * Sets the custom Light Level at the given position
     * </p>
     *
     * @param world           The {@link World} to set custom Light Level in
     * @param position        The {@link IntPosition} of the custom Light Source
     * @param customLuminance The custom Luminance to set
     * @param update          Whether Server and Client Light Updates should be performed immediately
     * @return A {@link CompletableFuture} returning a {@link LightUpdateResult} upon completion
     */
    CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull World world, @NotNull IntPosition position, int customLuminance, boolean update);

    /**
     * <p>
     * Same as {@link IVarLightAPI#setCustomLuminance(World, IntPosition, int)} but without the update parameter.<br/>
     * Calling this method will always immediately update Light on the Server and the Client on success (i.e. {@code true} is passed to {@link IVarLightAPI#setCustomLuminance(World, IntPosition, int, boolean)})
     * </p>
     *
     * @see IVarLightAPI#setCustomLuminance(World, IntPosition, int, boolean)
     */
    default CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull World world, @NotNull IntPosition position, int customLuminance) {
        return setCustomLuminance(world, position, customLuminance, true);
    }

    /**
     * <p>
     * Same as {@link IVarLightAPI#setCustomLuminance(World, IntPosition, int, boolean)} but taking a {@link Location} as an Argument rather than
     * a separate {@link World} and {@link IntPosition}
     * </p>>
     *
     * @see IVarLightAPI#setCustomLuminance(World, IntPosition, int, boolean)
     */
    default CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull Location location, int customLuminance, boolean update) {
        return setCustomLuminance(location.getWorld(), IntPositionExtension.toIntPosition(location), customLuminance, update);
    }

    /**
     * <p>
     * Same as {@link IVarLightAPI#setCustomLuminance(Location, int, boolean)} but without the update parameter.<br/>
     * Calling this method will always immediately update Light on the Server and the Client on success (i.e. {@code true} is passed to {@link IVarLightAPI#setCustomLuminance(Location, int, boolean)})
     * </p>
     */
    default CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull Location location, int customLuminance) {
        return setCustomLuminance(location, customLuminance, true);
    }

    /**
     * <p>
     * Same as {@link IVarLightAPI#setCustomLuminance(World, IntPosition, int)} but rather than returning the {@link CompletableFuture},
     * {@link LightUpdateResult#displayMessage(CommandSender)} is called using the specified {@link CommandSender} source.
     * </p>
     *
     * @see IVarLightAPI#setCustomLuminance(World, IntPosition, int)
     */
    void setCustomLuminance(@Nullable CommandSender source, @NotNull World world, @NotNull IntPosition position, int customLuminance);

    /**
     * <p>
     * Same as {@link IVarLightAPI#setCustomLuminance(CommandSender, World, IntPosition, int)} bur taking a {@link Location} as an Argument rather than a
     * separate {@link World} and {@link IntPosition}
     * </p>
     *
     * @see IVarLightAPI#setCustomLuminance(CommandSender, World, IntPosition, int)
     */
    default void setCustomLuminance(@Nullable CommandSender source, @NotNull Location location, int customLuminance) {
        setCustomLuminance(source, location.getWorld(), IntPositionExtension.toIntPosition(location), customLuminance);
    }

    /**
     * <p>
     * Same as {@link IVarLightAPI#setCustomLuminance(World, IntPosition, int, boolean)} but without the Light Level parameter.
     * {@code 0} will always be passed to {@link IVarLightAPI#setCustomLuminance(World, IntPosition, int, boolean)} (removing the custom Light Source)
     * </p>
     *
     * @see IVarLightAPI#setCustomLuminance(World, IntPosition, int, boolean)
     */
    default CompletableFuture<LightUpdateResult> clearCustomLuminance(@NotNull World world, @NotNull IntPosition position, boolean update) {
        return setCustomLuminance(world, position, 0, update);
    }

    /**
     * <p>
     * Same as {@link IVarLightAPI#clearCustomLuminance(World, IntPosition, boolean)} but light is always updated
     * ({@code true} is passed to {@link IVarLightAPI#clearCustomLuminance(World, IntPosition, boolean)})
     * </p>
     *
     * @see IVarLightAPI#clearCustomLuminance(World, IntPosition, boolean)
     */
    default CompletableFuture<LightUpdateResult> clearCustomLuminance(@NotNull World world, IntPosition position) {
        return clearCustomLuminance(world, position, true);
    }

    /**
     * <p>
     * Same as {@link IVarLightAPI#clearCustomLuminance(World, IntPosition, boolean)} but a {@link Location} is taken as an argument rather than
     * a separate {@link World} and {@link IntPosition}
     * </p>
     *
     * @see IVarLightAPI#clearCustomLuminance(World, IntPosition, boolean)
     */
    default CompletableFuture<LightUpdateResult> clearCustomLuminance(@NotNull Location location, boolean update) {
        return clearCustomLuminance(location.getWorld(), IntPositionExtension.toIntPosition(location), update);
    }

    /**
     * <p>
     * Same as {@link IVarLightAPI#clearCustomLuminance(Location, boolean)} but light is always updated
     * ({@code true} is passed to {@link IVarLightAPI#clearCustomLuminance(Location, boolean)})
     * </p>
     *
     * @see IVarLightAPI#clearCustomLuminance(Location, boolean)
     */
    default CompletableFuture<LightUpdateResult> clearCustomLuminance(@NotNull Location location) {
        return clearCustomLuminance(location, true);
    }

    /**
     * <p>
     * Schedules a Bulk Task to completely clear a larger area of custom Light Sources
     * </p>
     *
     * @param world  The world containing the Region
     * @param source The Command source that initiated the bulk clear
     * @param start  The start position
     * @param end    The end position
     * @return A {@link CompletableFuture} returning a {@link BulkTaskResult} upon completion
     */
    CompletableFuture<BulkTaskResult> runBulkClear(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end);

    /**
     * <p>
     * Schedules a Bulk Task to fill a certain area with custom Light Sources
     * </p>
     *
     * @param world      The world containing the region to fill
     * @param source     The Command source that initiated the bulk fill
     * @param start      The start position
     * @param end        The end position
     * @param lightLevel The light level to apply
     * @param filter     A Block {@link Predicate} used to determine which blocks are included
     * @return A {@link CompletableFuture} returning a {@link BulkTaskResult} upon completion
     */
    CompletableFuture<BulkTaskResult> runBulkFill(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end, int lightLevel, @Nullable Predicate<Block> filter);

    /**
     * <p>
     * Same as {@link IVarLightAPI#runBulkFill(World, CommandSender, IntPosition, IntPosition, int, Predicate)} but without the predicate argument.
     * {@code (block) -> true} is passed to {@link IVarLightAPI#runBulkFill(World, CommandSender, IntPosition, IntPosition, int, Predicate)}
     * </p>
     *
     * @see IVarLightAPI#runBulkFill(World, CommandSender, IntPosition, IntPosition, int, Predicate)
     */
    default CompletableFuture<BulkTaskResult> runBulkFill(@NotNull World world, @NotNull CommandSender source, @NotNull IntPosition start, @NotNull IntPosition end, int lightLevel) {
        return runBulkFill(world, source, start, end, lightLevel, x -> true);
    }

    /**
     * <p>
     * Internal API
     * </p>
     *
     * <p>
     * The Light Update Methods found in {@link ICustomLightStorage} only store the Custom Light Level in persistence. <strong>No Light Updates will be performed on the Server or Client</strong>
     * </p>
     */
    Internal unsafe();

    /**
     * <p>
     * Internal API
     * </p>
     *
     * <p>
     * The Light Update Methods found in {@link ICustomLightStorage} only store the Custom Light Level in persistence. <strong>No Light Updates will be performed on the Server or Client</strong>
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

        @NotNull Collection<ICustomLightStorage> getAllActiveVarLightWorlds();
    }
}
