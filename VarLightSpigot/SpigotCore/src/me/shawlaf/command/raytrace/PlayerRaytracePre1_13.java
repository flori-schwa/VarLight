package me.shawlaf.command.raytrace;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class PlayerRaytracePre1_13 {

    private PlayerRaytracePre1_13() {
        throw new UnsupportedOperationException();
    }

    static Block getTargetBlock(Player player, int maxDistance) {
        return player.getTargetBlock(new HashSet<>(), maxDistance);
    }
}
