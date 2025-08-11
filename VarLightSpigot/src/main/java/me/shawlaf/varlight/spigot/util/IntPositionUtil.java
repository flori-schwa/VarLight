package me.shawlaf.varlight.spigot.util;

import me.shawlaf.varlight.util.pos.IntPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class IntPositionUtil {

    private IntPositionUtil() {

    }

    public static IntPosition toIntPosition(Block self) {
        return new IntPosition(self.getX(), self.getY(), self.getZ());
    }

    public static IntPosition toIntPosition(Location self) {
        return new IntPosition(self.getBlockX(), self.getBlockY(), self.getBlockZ());
    }

    public static Location toLocation(IntPosition self, World world) {
        return new Location(world, self.x(), self.y(), self.z());
    }

    public static Block toBlock(IntPosition self, World world) {
        return self.convert(world::getBlockAt);
    }

}
