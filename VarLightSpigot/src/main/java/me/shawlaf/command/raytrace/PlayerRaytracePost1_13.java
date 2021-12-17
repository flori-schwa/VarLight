package me.shawlaf.command.raytrace;

import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class PlayerRaytracePost1_13 {

    private PlayerRaytracePost1_13() {
        throw new UnsupportedOperationException();
    }

    static Block getTargetBlock(Player player, int maxDistance) {
        return player.getTargetBlockExact(maxDistance, FluidCollisionMode.NEVER);
    }
}
