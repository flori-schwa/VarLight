package me.shawlaf.varlight.spigot.exceptions;

import lombok.Getter;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class VarLightNotActiveException extends Exception { // TODO maybe RuntimeExeption?

    @NotNull @Getter
    private final World world;

    public VarLightNotActiveException(@NotNull World world) {
        this.world = Objects.requireNonNull(world, "World may not be null");
    }

    @Override
    public String getMessage() {
        return String.format("VarLight is not active in World %s.", this.world.getName());
    }
}
