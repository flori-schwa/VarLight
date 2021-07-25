package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.INmsMethods;
import me.shawlaf.varlight.spigot.util.NamespacedID;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.LocaleLanguage;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.NBTTagInt;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joor.Reflect;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@ExtensionMethod({
        Util.class
})
public class NmsAdapter implements INmsMethods {

    private final VarLightPlugin plugin;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isIllegalBlock(@NotNull Block bukkitBlock) {
        return isIllegalBlockType(bukkitBlock.getType());
    }

    @Override
    public boolean isIllegalBlockType(@NotNull Material blockType) {
        if (HardcodedBlockList.ALLOWED_BLOCKS.contains(blockType)) {
            return false;
        }

        if (plugin.getVarLightConfig().isAllowExperimentalBlocks()) {
            return !HardcodedBlockList.EXPERIMENTAL_BLOCKS.contains(blockType);
        }

        return true;
    }

    @Override
    public boolean isIllegalLightUpdateItem(@NotNull Material material) {
        return material.isBlock() || !material.isItem();
    }

    @Override
    public boolean supportsPluginChunkTickets() {
        return true;
    }

    @Override
    public boolean hasPersistenceApi() {
        return true;
    }

    @Override
    public @NotNull File getRegionRoot(@NotNull World bukkitWorld) {
        return Reflect.on(bukkitWorld.toNmsWorld().getChunkProvider().playerChunkMap).get("w");
    }

    @Override
    public @NotNull NamespacedKey getBukkitKey(@NotNull Material material) {
        return material.getKey();
    }

    @Override
    public @Nullable Material getItemFromKey(@NotNull NamespacedID namespacedID) {
        return CraftMagicNumbers.getMaterial(IRegistry.ITEM.get(namespacedID.toMinecraftKey()));
    }

    @Override
    public @Nullable Material getBlockFromKey(@NotNull NamespacedID namespacedID) {
        MinecraftKey key = new MinecraftKey(namespacedID.getNamespace(), namespacedID.getName());
        return CraftMagicNumbers.getMaterial(IRegistry.BLOCK.get(namespacedID.toMinecraftKey()));
    }

    @Override
    public @NotNull Collection<NamespacedID> getAllMinecraftBlockKeys() {
        return IRegistry.BLOCK.keySet().stream().map((mcKey) -> mcKey.toNamespacedId()).collect(Collectors.toList());
    }

    @Override
    public @NotNull Collection<NamespacedID> getAllMinecraftItemKeys() {
        return IRegistry.ITEM.keySet().stream().map((mcKey) -> mcKey.toNamespacedId()).collect(Collectors.toList());
    }

    @Override
    public @NotNull ItemStack makeGlowingStack(@NotNull ItemStack base, int lightLevel) {
        net.minecraft.server.v1_16_R3.ItemStack nmsStack = new net.minecraft.server.v1_16_R3.ItemStack(
                CraftMagicNumbers.getItem(base.getType()),
                base.getAmount()
        );

        lightLevel &= 0xF;

        nmsStack.a("varlight:glowing", NBTTagInt.a(lightLevel));

        ItemStack stack = CraftItemStack.asBukkitCopy(nmsStack);

        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Glowing " + getLocalizedBlockName(stack.getType()));
        meta.setLore(Collections.singletonList(ChatColor.RESET + "Emitting Light: " + lightLevel));

        stack.setItemMeta(meta);

        return stack;
    }

    @Override
    public @NotNull String getLocalizedBlockName(Material type) {
        return LocaleLanguage.a().a(CraftMagicNumbers.getBlock(type).i());
    }
}
