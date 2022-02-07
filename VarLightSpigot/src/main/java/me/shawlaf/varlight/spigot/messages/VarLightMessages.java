package me.shawlaf.varlight.spigot.messages;

import lombok.experimental.UtilityClass;
import org.bukkit.World;

@UtilityClass
public class VarLightMessages {

    public String varLightNotActiveInWorld(World world) {
        return String.format("VarLight is not active in World %s.", world.getName());
    }

}
