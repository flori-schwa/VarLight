package me.shawlaf.varlight.spigot.persistence;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.exception.VarLightIOException;
import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.persistence.nls.common.exception.PositionOutOfBoundsException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import me.shawlaf.varlight.util.pos.RegionCoords;
import me.shawlaf.varlight.util.pos.RegionIterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

@ExtensionMethod({
        IntPositionExtension.class
})
public class CustomLightStorageNLS implements ICustomLightStorage {

    @Getter
    private final World forBukkitWorld;
    @Getter
    private final VarLightPlugin plugin;

    private final Map<RegionCoords, NLSFile> worldMap = new HashMap<>();

    public CustomLightStorageNLS(World forBukkitWorld, VarLightPlugin plugin) {
        this.forBukkitWorld = forBukkitWorld;
        this.plugin = plugin;

        plugin.getNmsAdapter().getVarLightSaveDirectory(forBukkitWorld);
        plugin.getLightDatabaseMigrator().runMigrations(forBukkitWorld);
    }

    @Override
    public int getCustomLuminance(IntPosition position, int def) {
        int lum;

        try {
            lum = getNLSFile(position.toRegionCoords()).getCustomLuminance(position);
        } catch (PositionOutOfBoundsException e) {
            return def;
        }

        if (lum == 0) {
            return def;
        }

        return lum;
    }

    @Override
    public boolean hasChunkCustomLightData(ChunkCoords chunkCoords) {
        return getNLSFile(chunkCoords.toRegionCoords()).hasChunkData(chunkCoords);
    }

    @Override
    public Iterator<IntPosition> iterateAllLightSources(IntPosition a, IntPosition b) {
        RegionIterator iterator = new RegionIterator(a, b);

        return new Iterator<IntPosition>() {

            IntPosition next;

            {
                findNext();
            }

            private void findNext() {
                next = null;

                while (iterator.hasNext()) {
                    IntPosition pos = iterator.next();

                    if (getCustomLuminance(pos) != 0) {
                        next = pos;
                        break;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public IntPosition next() {
                IntPosition tmp = next;
                findNext();
                return tmp;
            }
        };
    }

    @Override
    public int setCustomLuminance(Location location, int luminance) throws PositionOutOfBoundsException {
        return setCustomLuminance(location.toIntPosition(), luminance);
    }

    @Override
    public int setCustomLuminance(IntPosition position, int luminance) throws PositionOutOfBoundsException {
        return getNLSFile(position.toRegionCoords()).setCustomLuminance(position, luminance);
    }

    @Override
    public void clearChunk(ChunkCoords chunkCoords) {
        getNLSFile(chunkCoords.toRegionCoords()).clearChunk(chunkCoords);
    }

    @Override
    public void runAutosave() {
        save(Bukkit.getConsoleSender(), plugin.getVarLightConfig().isLogVerbose());
    }

    @Override
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
