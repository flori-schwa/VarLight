package me.shawlaf.varlight.spigot.persistence;

import lombok.Getter;
import me.shawlaf.varlight.exception.VarLightIOException;
import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.persistence.nls.exception.PositionOutOfBoundsException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoords;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

public class WorldLightPersistence {

    @Getter
    private final World forBukkitWorld;
    @Getter
    private final VarLightPlugin plugin;

    private final Map<RegionCoords, NLSFile> worldMap = new HashMap<>();

    public WorldLightPersistence(World forBukkitWorld, VarLightPlugin plugin) {
        this.forBukkitWorld = forBukkitWorld;
        this.plugin = plugin;

        // TODO ensure save directory exists and run migrations
    }

    public int getCustomLuminance(IntPosition position, int def) {
        return getCustomLuminance(position, () -> def);
    }

    public int getCustomLuminance(IntPosition position, IntSupplier def) {
        int lum;

        try {
            lum = getNLSFile(position.toRegionCoords()).getCustomLuminance(position);
        } catch (PositionOutOfBoundsException e) {
            return def.getAsInt();
        }

        if (lum == 0) {
            return def.getAsInt();
        }

        return lum;
    }

    public boolean hasChunkCustomLightData(ChunkCoords chunkCoords) {
        return getNLSFile(chunkCoords.toRegionCoords()).hasChunkData(chunkCoords);
    }

    public void setCustomLuminance(Location location, int luminance) throws PositionOutOfBoundsException {
        setCustomLuminance(toIntPosition(location), luminance);
    }

    public void setCustomLuminance(IntPosition position, int luminance) throws PositionOutOfBoundsException {
        getNLSFile(position.toRegionCoords()).setCustomLuminance(position, luminance);
    }

    public void save(CommandSender commandSender, boolean log) {
        int modified = 0, deleted = 0;
        List<RegionCoords> regionsToUnload = new ArrayList<>();

        synchronized (worldMap) {
            for (NLSFile nlsFile : worldMap.values()) {

                try {
                    if (nlsFile.save()) {
                        ++modified;
                    }
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                List<ChunkCoords> affected = nlsFile.getAffectedChunks();

                if (affected.size() == 0) {
                    if (nlsFile.file.exists()) {
                        if (!nlsFile.file.delete()) {
                            throw new LightPersistFailedException("Could not delete file " + nlsFile.file.getAbsolutePath());
                        } else {
//                            if (log) { TODO implement and localize
//                                CommandResult.info(plugin.getCommand(), commandSender, String.format("Deleted File %s", nlsFile.file.getName()));
//                            }

                            ++deleted;
                        }
                    }

                    regionsToUnload.add(nlsFile.getRegionCoords());
                    continue;
                }

                boolean anyLoaded = false;

                for (ChunkCoords chunkCoords : affected) {
                    if (forBukkitWorld.isChunkLoaded(chunkCoords.x, chunkCoords.z)) {
                        anyLoaded = true;
                        break;
                    }
                }

                if (!anyLoaded) {
                    regionsToUnload.add(nlsFile.getRegionCoords());
                }
            }

            for (RegionCoords regionCoords : regionsToUnload) {
                worldMap.remove(regionCoords).unload();
            }
        }

        if (log) {
            commandSender.sendMessage(String.format("[VarLight] Light Sources persisted for World \"%s\", Files modified: %d, Files deleted: %d", forBukkitWorld.getName(), modified, deleted)); // TODO localize
        }
    }

    @NotNull
    public NLSFile getNLSFile(RegionCoords regionCoords) {
        synchronized (worldMap) {
            if (!worldMap.containsKey(regionCoords)) {
                File file = NLSFile.getFile(plugin.getNmsAdapter().getVarLightSaveDirectory(this.forBukkitWorld), regionCoords);
                NLSFile nlsFile;

                try {
                    if (file.exists()) {
                        nlsFile = NLSFile.existingFile(file, plugin.getVarLightConfig().shouldDeflate());
                    } else {
                        nlsFile = NLSFile.newFile(file, regionCoords.x, regionCoords.z, plugin.getVarLightConfig().shouldDeflate());
                    }
                } catch (IOException e) {
                    throw new VarLightIOException(e);
                }

                worldMap.put(regionCoords, nlsFile);
            }
        }

        return worldMap.get(regionCoords);
    }

}
