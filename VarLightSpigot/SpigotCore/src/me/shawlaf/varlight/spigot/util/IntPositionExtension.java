package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@UtilityClass
public class IntPositionExtension {

    public IntPosition toIntPosition(Block self) {
        return new IntPosition(self.getX(), self.getY(), self.getZ());
    }

    public IntPosition toIntPosition(Location self) {
        return new IntPosition(self.getBlockX(), self.getBlockY(), self.getBlockZ());
    }

    public Block toBlock(IntPosition self, World world) {
        return world.getBlockAt(self.x, self.y, self.z);
    }

}
