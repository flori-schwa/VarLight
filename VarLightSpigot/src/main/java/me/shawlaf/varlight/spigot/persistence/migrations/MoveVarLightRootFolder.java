package me.shawlaf.varlight.spigot.persistence.migrations;

import me.shawlaf.varlight.persistence.migrate.Migration;
import me.shawlaf.varlight.persistence.migrate.MigrationFailedException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MoveVarLightRootFolder implements Migration<World> {

    private final VarLightPlugin plugin;

    public MoveVarLightRootFolder(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean migrate(World world) {
        if (world.getEnvironment() == World.Environment.NORMAL) {
            return false;
        }

        File oldVarLightFolder = new File(world.getWorldFolder(), "varlight");
        File newVarLightFolder = plugin.getNmsAdapter().getVarLightSaveDirectory(world);

        if (oldVarLightFolder.equals(newVarLightFolder)) {
            return false;
        }

        if (oldVarLightFolder.exists() && oldVarLightFolder.isDirectory()) {
            File[] files = oldVarLightFolder.listFiles();

            if (files != null && files.length > 0) {
                try {
                    Files.move(oldVarLightFolder.toPath(), newVarLightFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (IOException e) {
                    throw new MigrationFailedException(this, world.getName(), e);
                }
            } else {
                return oldVarLightFolder.delete();
            }
        }

        return false;
    }
}
