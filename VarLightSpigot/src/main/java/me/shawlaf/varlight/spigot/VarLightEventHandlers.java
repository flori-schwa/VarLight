package me.shawlaf.varlight.spigot;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.command.result.CommandResult;
import me.shawlaf.varlight.spigot.api.LightUpdateResult;
import me.shawlaf.varlight.spigot.async.Ticks;
import me.shawlaf.varlight.spigot.event.LightUpdateCause;
import me.shawlaf.varlight.spigot.glowingitems.GlowItemStack;
import me.shawlaf.varlight.spigot.messages.VarLightMessages;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.spigot.util.VarLightPermissions;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

@ExtensionMethod({
        IntPositionExtension.class,
        VarLightPermissions.class
})
public class VarLightEventHandlers implements Listener {

    @Getter
    private final VarLightPlugin plugin;

    public VarLightEventHandlers(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerInspectLightSource(PlayerInteractEvent e) {
        if (!plugin.getNmsAdapter().isVarLightDebugStick(e.getItem())) {
            return;
        }

        e.setCancelled(true);

        if (e.getClickedBlock() == null) {
            return;
        }

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!e.getPlayer().hasVarLightDebugPermission()) {
            CommandResult.failure(plugin.getCommand(), e.getPlayer(), "You do not have permission to use the debug stick!");
            return;
        }

        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(e.getClickedBlock().getWorld())) == null) {
            CommandResult.failure(plugin.getCommand(), e.getPlayer(), VarLightMessages.varLightNotActiveInWorld(e.getClickedBlock().getWorld()));
            return;
        }

        IntPosition clickedBlock = e.getClickedBlock().toIntPosition();

        int customLuminance = cls.getCustomLuminance(clickedBlock);

        if (customLuminance == 0) {
            CommandResult.info(plugin.getCommand(), e.getPlayer(), String.format("No custom light source present at Position %s", clickedBlock.toShortString()), ChatColor.RED);
        } else {
            CommandResult.info(plugin.getCommand(), e.getPlayer(), String.format("Custom Light Level of Block at Position %s: %d", clickedBlock.toShortString(), customLuminance), ChatColor.GREEN);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void playerModifyLightSource(PlayerInteractEvent e) {
        ICustomLightStorage cls = plugin.getApi().unsafe().getLightStorage(e.getPlayer().getWorld());

        if (cls == null) {
            return;
        }

        if (e.useInteractedBlock() == Event.Result.DENY // Check for Spawn Protection
                || (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        if (!e.getPlayer().mayUseLui()) {
            return;
        }

        Block clicked = e.getClickedBlock();
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        final boolean creative = player.getGameMode() == GameMode.CREATIVE;

        // Check if proper item was used
        if (item == null || item.getType() != plugin.getApi().getLightUpdateItem()) {
            return;
        }

        int mod = 0;

        switch (e.getAction()) {
            case RIGHT_CLICK_BLOCK:
                mod = 1;
                break;
            case LEFT_CLICK_BLOCK:
                mod = -1;
                break;
        }

        // Check if sufficient amount when not in creative
        if (!creative && mod > 0 && item.getAmount() < mod) {
            return;
        }

        if (plugin.getVarLightConfig().isAllowedStepsizeGamemode(player.getGameMode())) {
            mod *= plugin.getApi().getStepsizeManager().getStepSize(player);
        }

        final int finalMod = mod;

        e.setCancelled(creative && e.getAction() == Action.LEFT_CLICK_BLOCK); // Prevent Block break in creative

        plugin.getApi().setCustomLuminance(clicked.getLocation(), cls.getCustomLuminance(clicked.toIntPosition()) + mod, true, LightUpdateCause.player(player)).thenAccept(result -> {
            if (result.isSuccess()) {

                if (plugin.getVarLightConfig().isConsumeLui() && !creative && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    item.setAmount(item.getAmount() - Math.abs(finalMod));
                }
            }

            result.displayMessage(player);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void playerBreakLightSource(BlockBreakEvent e) {
        if (!plugin.getVarLightConfig().isReclaimEnabled()) {
            return;
        }

        if (!e.getPlayer().mayReclaimLui()) {
            return;
        }

        Block theBlock = e.getBlock();
        IntPosition position = theBlock.toIntPosition();
        World world = theBlock.getWorld();

        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(world)) == null) {
            return;
        }

        int customLuminance = cls.getCustomLuminance(position);

        if (customLuminance <= 0) {
            return;
        }

        // Can't break blocks using off-hand
        ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();

        int fortuneLevel = heldItem.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
        boolean silkTouch = heldItem.getEnchantmentLevel(Enchantment.SILK_TOUCH) != 0;

        Collection<ItemStack> vanillaDrops = theBlock.getDrops(heldItem);

        if (silkTouch) {
            if (vanillaDrops.size() != 1 || vanillaDrops.stream().findFirst().get().getAmount() != 1) {
                return;
            }

            ItemStack vanillaDrop = vanillaDrops.stream().findFirst().get();

            e.setDropItems(false);
            world.dropItemNaturally(theBlock.getLocation(), plugin.getApi().createGlowItemStack(vanillaDrop, customLuminance).getItemStack());
        } else {
            if (!plugin.getVarLightConfig().isConsumeLui()) {
                return; // Prevent infinite duping of LUI
            }

            if (vanillaDrops.size() == 0) {
                return;
            }

            ItemStack luiStack = new ItemStack(plugin.getApi().getLightUpdateItem(), 1);

            if (fortuneLevel == 0) {
                world.dropItemNaturally(theBlock.getLocation(), luiStack);
            } else {
                // f(x) = 1 - (1 - -0.5) * e^(-0.6 * x)
                double chance = 1d - (1.5) * Math.exp(-0.6 * fortuneLevel);

                for (int i = 0; i < customLuminance; i++) {
                    if (Math.random() <= chance) {
                        world.dropItemNaturally(theBlock.getLocation(), luiStack);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerPlaceLightSource(BlockPlaceEvent e) {
        if (!e.getPlayer().mayReclaimLui() || !e.canBuild()) {
            return;
        }

        // noinspection ConstantConditions: https://github.com/flori-schwa/VarLightOld/issues/24
        if (e.getItemInHand() == null) {
            return;
        }

        if (!plugin.getApi().isVarLightEnabled(e.getBlock().getWorld())) {
            CommandResult.info(plugin.getCommand(), e.getPlayer(), "VarLight is not active in your current world!");
            e.setCancelled(true);
            return;
        }

        ItemStack handCopy = e.getItemInHand().clone();
        final GlowItemStack placedItemStack = plugin.getApi().importGlowItemStack(handCopy);

        if (placedItemStack == null) {
            return;
        }

        final Material before = e.getBlock().getType();

        if (placedItemStack.getCustomLuminance() > 0) {
            plugin.getApi().getAsyncExecutor().submitDelayed(() -> {
                LightUpdateResult result = plugin.getApi().setCustomLuminance(e.getBlock().getLocation(), placedItemStack.getCustomLuminance(), true, LightUpdateCause.player(e.getPlayer())).join();

                if (!result.isSuccess()) {
                    plugin.getApi().getSyncExecutor().submit(() -> {
                        e.getBlock().setType(before);

                        handCopy.setAmount(1);
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), handCopy);
                    });
                }
            }, Ticks.of(1));
        }

    }

    @EventHandler
    public void lightSourceReceiveUpdate(BlockPhysicsEvent e) {
        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(e.getBlock().getWorld())) == null) {
            return;
        }

        IntPosition position = e.getBlock().toIntPosition();
        int luminance = cls.getCustomLuminance(position);

        if (luminance > 0) {
            if (e.getBlock() == e.getSourceBlock()) {
                // The Light Source Block was changed

                // See World.notifyAndUpdatePhysics(BlockPosition, Chunk, IBlockData, IBlockData, IBlockData, int)
                if (plugin.getNmsAdapter().isIllegalBlockType(e.getChangedType())) {
                    cls.setCustomLuminance(position, 0);
                } else {
                    // Probably not possible, but /shrug
                    plugin.getLightUpdater().updateLightSingleBlock(cls, position);
                }
            } else {
                // The Light source Block received an update from another Block
                plugin.getLightUpdater().updateLightSingleBlock(cls, position);
            }
        }
    }

}
