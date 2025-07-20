package me.shawlaf.varlight.spigot.messages;

import org.bukkit.World;

public final class VarLightMessages {

    private VarLightMessages() {

    }

    public static String varLightNotActiveInWorld(World world) {
        return String.format("VarLight is not active in World %s.", world.getName());
    }

}
