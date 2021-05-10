package me.shawlaf.command.raytrace;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class PlayerRaytrace {

    private PlayerRaytrace() {
        throw new UnsupportedOperationException();
    }

    public static Block getTargetBlock(Player player, int maxDistance) {
        try {
            Class.forName("org.bukkit.FluidCollisionMode");

            return PlayerRaytracePost1_13.getTargetBlock(player, maxDistance);
        } catch (ClassNotFoundException e) {
            return PlayerRaytracePre1_13.getTargetBlock(player, maxDistance);
        }
    }

}
