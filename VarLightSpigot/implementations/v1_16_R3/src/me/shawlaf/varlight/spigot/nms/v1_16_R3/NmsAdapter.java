package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import me.shawlaf.varlight.spigot.nms.INmsMethods;
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

public class NmsAdapter implements INmsMethods {
    @Override
    public boolean isIllegalBlock(@NotNull Block bukkitBlock) {
        return false;
    }

    @Override
    public boolean isIllegalLightUpdateItem(@NotNull Material material) {
        return false;
    }

    @Override
    public boolean supportsPluginChunkTickets() {
        return false;
    }

    @Override
    public @NotNull File getRegionRoot(@NotNull World bukkitWorld) {
        return null;
    }

    @Override
    public @NotNull NamespacedKey getBukkitKey(@NotNull Material material) {
        return null;
    }

    @Override
    public @Nullable Material getItemFromKey(@NotNull NamespacedID namespacedID) {
        return null;
    }

    @Override
    public @Nullable Material getBlockFromKey(@NotNull NamespacedID namespacedID) {
        return null;
    }

    @Override
    public @NotNull Collection<NamespacedID> getAllMinecraftBlockKeys() {
        return null;
    }

    @Override
    public @NotNull Collection<NamespacedID> getAllMinecraftItemKeys() {
        return null;
    }

    @Override
    public @NotNull ItemStack makeGlowingStack(@NotNull ItemStack base, int lightLevel) {
        return null;
    }

    @Override
    public @NotNull String getLocalizedBlockName(Material type) {
        return null;
    }
}
