package me.shawlaf.varlight.spigot;

import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.WorldLightPersistence;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class VarLightPlugin extends JavaPlugin {

    private Map<UUID, WorldLightPersistence> persistenceManagers = new HashMap<>();

    public WorldLightPersistence requireVarLightEnabled(@NotNull World world) throws VarLightNotActiveException {
        WorldLightPersistence wlp = persistenceManagers.get(Objects.requireNonNull(world, "World may not be null").getUID());

        if (wlp == null) {

            // TODO check if Persistence manager should be created

            throw new VarLightNotActiveException(world);
        }

        return wlp;
    }

}
