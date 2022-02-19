package me.shawlaf.varlight.spigot.persistence.migrations;

import me.shawlaf.varlight.persistence.migrate.Migration;
import me.shawlaf.varlight.persistence.migrate.MigrationFailedException;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.persistence.old.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.old.vldb.VLDBInputStream;
import me.shawlaf.varlight.persistence.old.vldb.VLDBUtil;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.util.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class VLDBToNLSMigration implements Migration<File> {

    private final VarLightPlugin plugin;

    public VLDBToNLSMigration(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean migrate(File file) throws Exception {
        boolean isVLDBOld = file.getName().toLowerCase().endsWith(".vldb");
        boolean isVLDBNew = file.getName().toLowerCase().endsWith(".vldb2");

        boolean isVLDB = isVLDBNew || isVLDBOld;

        if (!isVLDB) {
            return false;
        }

        try (InputStream is = FileUtil.openStreamInflate(file)) {
            VLDBInputStream in = new VLDBInputStream(is);

            if (isVLDBNew) {
                if (!in.readVLDBMagic()) {
                    throw new RuntimeException("Malformed VLDB File " + file.getAbsolutePath());
                }
            }

            int regionX = in.readInt32();
            int regionZ = in.readInt32();

            NLSFile nlsFile = NLSFile.newFile(new File(file.getParentFile().getAbsoluteFile(), String.format(NLSFile.FILE_NAME_FORMAT, regionX, regionZ)), regionX, regionZ, plugin.getVarLightConfig().shouldDeflate());

            int amountChunks = in.readInt16();

            in.skip(amountChunks * VLDBUtil.SIZEOF_OFFSET_TABLE_ENTRY);

            for (int i = 0; i < amountChunks; ++i) {
                for (BasicCustomLightSource bcls : in.readChunk(regionX, regionZ, BasicCustomLightSource[]::new, BasicCustomLightSource::new).item2) {
                    nlsFile.setCustomLuminance(bcls.getPosition(), bcls.getCustomLuminance());
                }
            }

            nlsFile.saveAndUnload();
            in.close();
        }

        if (!file.delete()) {
            throw new MigrationFailedException(this, file, "Could not delete the VLDB File");
        }

        return true;
    }
}
