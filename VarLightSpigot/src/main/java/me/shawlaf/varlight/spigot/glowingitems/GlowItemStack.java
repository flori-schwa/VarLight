package me.shawlaf.varlight.spigot.glowingitems;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class GlowItemStack {

    private final ItemStack itemStack;
    private final int customLuminance;

    public GlowItemStack(@NotNull VarLightPlugin plugin, @NotNull ItemStack baseStack, int lightLevel) {
        this.customLuminance = lightLevel;
        this.itemStack = plugin.getNmsAdapter().makeGlowingStack(Objects.requireNonNull(baseStack), lightLevel);
    }

    public GlowItemStack(@NotNull VarLightPlugin plugin, @NotNull ItemStack importStack) {
        this.itemStack = Objects.requireNonNull(importStack);
        this.customLuminance = plugin.getNmsAdapter().getGlowingValue(this.itemStack);

        if (this.customLuminance < 0) {
            throw new IllegalArgumentException("Supplied Itemstack is not a glowing item!");
        }
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getCustomLuminance() {
        return customLuminance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlowItemStack that = (GlowItemStack) o;
        return customLuminance == that.customLuminance && itemStack.equals(that.itemStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemStack, customLuminance);
    }
}
