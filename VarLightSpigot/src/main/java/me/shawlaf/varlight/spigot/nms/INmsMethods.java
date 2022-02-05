package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.exception.VarLightIOException;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import me.shawlaf.varlight.spigot.util.NamespacedID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

/**
 * Interface containing methods with minecraft version-specific implementations
 */
public interface INmsMethods extends IPluginLifeCycleOperations {

    /**
     * Checks whether the specified {@link Block} is allowed to be turned into a Custom Light source
     *
     * @param bukkitBlock The {@link Block} to check
     * @return {@code true} if the Block is not allowed to be turned into a Custom Light source, {@code false} otherwise
     */
    boolean isIllegalBlock(@NotNull Block bukkitBlock);

    boolean isIllegalBlockType(@NotNull Material blockType);

    /**
     * Checks whether the specified {@link Material} is allowed to be used the Light Update Item (LUI)
     * @param material The {@link Material} to check
     * @return {@code true} if the Material is not allowed as the LUI, {@code false} otherwise.
     */
    boolean isIllegalLightUpdateItem(@NotNull Material material);

    /**
     * Returns the parent directory of the {@code region} folder stored inside minecraft worlds for the specified {@link World}
     *
     * @param bukkitWorld The world to get the {@code region} Folder's parent directory of.
     * @return The {@link File} object representing the directory containing the specified worlds {@code region} folder.
     */
    @NotNull File getRegionRoot(@NotNull World bukkitWorld);

    /**
     * Obtains a {@link NamespacedKey} containing the Namespaced ID of the specified Material,
     * for 1.13+, this is simply equivalent to {@code Material#getKey() }, this method however doesn't exist in 1.12
     * @param material The {@link Material} to get the Namespaced ID from
     * @return The {@link NamespacedKey} containing the Namespaced ID of the specified {@link Material}
     */
    @NotNull NamespacedKey getBukkitKey(@NotNull Material material);

    /**
     * Obtains the {@link Material} represented by the specified Namespaced ID
     * @param namespacedID The Namespaced ID
     * @return The associated Material, or null if none found
     */
    @Nullable Material getItemFromKey(@NotNull NamespacedID namespacedID);

    @Nullable Material getBlockFromKey(@NotNull NamespacedID namespacedID);

    @NotNull default NamespacedID getKey(@NotNull Material material) {
        return NamespacedID.fromBukkit(getBukkitKey(material));
    }

    @NotNull Collection<NamespacedID> getAllMinecraftBlockKeys();

    @NotNull Collection<NamespacedID> getAllMinecraftItemKeys();

    @NotNull ItemStack makeGlowingStack(@NotNull ItemStack base, int lightLevel);

    int getGlowingValue(@NotNull ItemStack glowingStack);

    @NotNull ItemStack makeVarLightDebugStick();

    boolean isVarLightDebugStick(ItemStack itemStack);

    @NotNull String getLocalizedBlockName(Material type);

    /**
     * Determines and creates (if not present) the directory where NLS Files are to be saved for the specified {@link World}.
     * This directory should share the same parent directory as the {@code region} directory containing the {@code .mca} files
     *
     * @param bukkitWorld The {@link World} to get the VarLight save directory for.
     * @return The {@link File} object representing the directory where NLS Files should be saved.
     */
    default File getVarLightSaveDirectory(@NotNull World bukkitWorld) {
        Objects.requireNonNull(bukkitWorld, "Must supply a world");

        File dir = new File(getRegionRoot(bukkitWorld), "varlight");

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new VarLightIOException(String.format("Could not create Varlight directory \"%s\"for world \"%s\"", dir.getAbsolutePath(), bukkitWorld.getName()));
            }
        }

        return dir;
    }

}
