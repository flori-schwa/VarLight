package me.shawlaf.varlight.spigot;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.WorldLightPersistence;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;

@ExtensionMethod({
        IntPositionExtension.class
})
public class VarLightEventHandlers implements Listener {

    @Getter
    private final VarLightPlugin plugin;

    public VarLightEventHandlers(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerModifyLightSource(PlayerInteractEvent e) {
        if (e.getItem() != null && e.getItem().getType() == plugin.getApi().getLightUpdateItem()) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
                e.getPlayer().sendMessage(ChatColor.RED + "This VarLight feature is not yet implemented");
            }
        }
    }

    @EventHandler
    public void lightSourceReceiveUpdate(BlockPhysicsEvent e) {
        WorldLightPersistence wlp;

        try {
            wlp = plugin.getApi().requireVarLightEnabled(e.getBlock().getWorld());
        } catch (VarLightNotActiveException ex) {
            return;
        }

        IntPosition position = e.getBlock().toIntPosition();
        int luminance = wlp.getCustomLuminance(position, 0);

        if (luminance > 0) {
            if (e.getBlock() == e.getSourceBlock()) {
                // The Light Source Block was changed

                // See World.notifyAndUpdatePhysics(BlockPosition, Chunk, IBlockData, IBlockData, IBlockData, int)
                if (plugin.getNmsAdapter().isIllegalBlockType(e.getChangedType())) {
                    wlp.setCustomLuminance(position, 0);
                } else {
                    // Probably not possible, but /shrug

                    plugin.getApi().getSyncExecutor().submit(() -> {
                        try {
                            plugin.getLightUpdater().updateLightServer(e.getBlock().getChunk());
                            plugin.getLightUpdater().updateLightClient(e.getBlock().getChunk());
                        } catch (VarLightNotActiveException varLightNotActiveException) {
                            varLightNotActiveException.printStackTrace();
                        }
                    });
                }
            } else {
                // The Light source Block received an update from another Block

                plugin.getApi().getSyncExecutor().submit(() -> {
                    try {
                        plugin.getLightUpdater().updateLightServer(e.getBlock().getChunk());
                        plugin.getLightUpdater().updateLightClient(e.getBlock().getChunk());
                    } catch (VarLightNotActiveException varLightNotActiveException) {
                        varLightNotActiveException.printStackTrace();
                    }
                });
            }
        }
    }

}
