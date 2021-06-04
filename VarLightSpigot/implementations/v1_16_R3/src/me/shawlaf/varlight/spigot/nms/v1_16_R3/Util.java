package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.spigot.util.NamespacedID;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import org.bukkit.NamespacedKey;

@UtilityClass
public class Util {

    public MinecraftKey toMinecraftKey(NamespacedID self) {
        return new MinecraftKey(self.getNamespace(), self.getName());
    }

    public NamespacedID toNamespacedId(NamespacedKey self) {
        return new NamespacedID(self.getNamespace(), self.getKey());
    }

    public NamespacedID toNamespacedId(MinecraftKey self) {
        return new NamespacedID(self.getNamespace(), self.getKey());
    }

}
