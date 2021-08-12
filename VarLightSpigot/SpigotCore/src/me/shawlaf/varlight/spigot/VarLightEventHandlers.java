package me.shawlaf.varlight.spigot;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.spigot.util.VarLightPermissions;
import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

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

    @EventHandler
    public void playerModifyLightSource(PlayerInteractEvent e) {
        ICustomLightStorage cls = plugin.getApi().getLightStorage(e.getPlayer().getWorld());

        if (cls == null) {
            return;
        }

        if (e.useInteractedBlock() == Event.Result.DENY // Check for Spawn Protection
                || (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        if (plugin.getVarLightConfig().isCheckingPermission() && !e.getPlayer().hasVarLightUsePermission()) {
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

        plugin.getApi().setCustomLuminance(clicked.getLocation(), cls.getCustomLuminance(clicked.toIntPosition(), 0) + mod).thenAccept(result -> {
            if (result.isSuccess()) {

                if (plugin.getVarLightConfig().isConsumeLui() && !creative && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    item.setAmount(item.getAmount() - Math.abs(finalMod));
                }
            }

            result.displayMessage(player);
        });
    }

    @EventHandler
    public void lightSourceReceiveUpdate(BlockPhysicsEvent e) {
        try {
            ICustomLightStorage cls = plugin.getApi().requireVarLightEnabled(e.getBlock().getWorld());

            IntPosition position = e.getBlock().toIntPosition();
            int luminance = cls.getCustomLuminance(position, 0);

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
        } catch (VarLightNotActiveException ex) {
            return;
        }
    }

}
