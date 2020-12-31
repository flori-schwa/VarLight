package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Location;

@UtilityClass
public class IntPositionExtension {

    public IntPosition toIntPosition(Location self) {
        return new IntPosition(self.getBlockX(), self.getBlockY(), self.getBlockZ());
    }

}
