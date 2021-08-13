package me.shawlaf.varlight.persistence.nls.common.migrate;

import me.shawlaf.varlight.persistence.nls.common.NLSHeader;
import me.shawlaf.varlight.util.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NLSMigrators {

    private static final Map<Integer, INLSMigrator> MIGRATORS = new HashMap<>();

    static {
        // Register Migrators here
    }

    public static void migrateFileToVersion(int targetVersion, File file) throws IOException {
        NLSHeader header;

        try (InputStream in = FileUtil.openStreamInflate(file)) {
            header = NLSHeader.readFromStream(in);
        }

        header.validRequired();

        if (header.getVersion() >= targetVersion) {
            throw new IllegalStateException(String.format("Target version already reached/exceeded: Attempted to migrate to version %d, but file is version: %d", targetVersion, header.getVersion()));
        }

        while (header.getVersion() < targetVersion) {
            INLSMigrator migrator = MIGRATORS.get(header.getVersion() + 1);
            Objects.requireNonNull(migrator, String.format("No NLS migrator found for Version %d -> %d", header.getVersion(), header.getVersion() + 1));

            header = migrator.migrateFile(file);
        }

    }

}
