package me.shawlaf.varlight.spigot.glowingitems;

import lombok.Getter;
import lombok.NonNull;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class GlowItemStack {

    @Getter
    private final ItemStack itemStack;
    @Getter
    private final int customLuminance;

    public GlowItemStack(@NonNull VarLightPlugin plugin, @NonNull ItemStack baseStack, int lightLevel) {
        this.customLuminance = lightLevel;
        this.itemStack = plugin.getNmsAdapter().makeGlowingStack(baseStack, lightLevel);
    }

    public GlowItemStack(@NonNull VarLightPlugin plugin, @NonNull ItemStack importStack) {
        this.itemStack = importStack;
        this.customLuminance = plugin.getNmsAdapter().getGlowingValue(this.itemStack);

        if (this.customLuminance < 0) {
            throw new IllegalArgumentException("Supplied Itemstack is not a glowing item!");
        }
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
