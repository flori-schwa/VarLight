package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.INmsMethods;
import me.shawlaf.varlight.spigot.util.NamespacedID;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joor.Reflect;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@ExtensionMethod({
        Util.class
})
public class NmsAdapter implements INmsMethods {

    private final VarLightPlugin plugin;

    private final ItemStack varLightDebugStick;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;

        net.minecraft.server.v1_16_R3.ItemStack nmsStack = new net.minecraft.server.v1_16_R3.ItemStack(Items.STICK);

        nmsStack.addEnchantment(Enchantments.DURABILITY, 1);
        nmsStack.a("CustomType", NBTTagString.a("varlight:debug_stick"));

        this.varLightDebugStick = CraftItemStack.asBukkitCopy(nmsStack);
        ItemMeta meta = varLightDebugStick.getItemMeta();

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "VarLight Debug Stick");
        varLightDebugStick.setItemMeta(meta);
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
    public boolean isVarLightDebugStick(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.STICK) {
            return false;
        }

        net.minecraft.server.v1_16_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null) {
            return false;
        }

        return tag.getString("CustomType").equals("varlight:debug_stick");
    }

    @Override
    public @NotNull ItemStack makeVarLightDebugStick() {
        net.minecraft.server.v1_16_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(varLightDebugStick);

        UUID id = UUID.randomUUID();

        nmsStack.getOrCreateTag().a("id", id);

        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public @NotNull String getLocalizedBlockName(Material type) {
        return LocaleLanguage.a().a(CraftMagicNumbers.getBlock(type).i());
    }
}
