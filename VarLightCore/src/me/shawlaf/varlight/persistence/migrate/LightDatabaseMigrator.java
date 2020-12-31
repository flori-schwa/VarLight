package me.shawlaf.varlight.persistence.migrate;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public abstract class LightDatabaseMigrator<W> {

    private final List<Migration<File>> dataMigrations = new ArrayList<>();
    private final List<Migration<W>> structureMigrations = new ArrayList<>();

    private final Logger logger;

    public LightDatabaseMigrator(Logger logger) {
        this.logger = logger;
    }

    public void addDataMigrations(Migration<File>... migrations) {
        dataMigrations.addAll(Arrays.asList(migrations));
    }

    public void addStructureMigrations(Migration<W>... migrations) {
        structureMigrations.addAll(Arrays.asList(migrations));
    }

    protected abstract File getVarLightSaveDirectory(W world);

    protected abstract String getName(W world);

    public void runMigrations(W world) {
        boolean or;

        do {
            or = false;

            for (Migration<W> structureMigration : structureMigrations) {
                try {
                    if (structureMigration.migrate(world)) {
                        logger.info(String.format("[%s] Migrated World \"%s\"", structureMigration.getName(), getName(world)));

                        or = true;
                    }
                } catch (Exception e) {
                    throw new MigrationFailedException(structureMigration, getName(world), e);
                }
            }
        } while (or);

        File saveDir = getVarLightSaveDirectory(world);

        do {
            or = false;

            File[] files = saveDir.listFiles();

            if (files == null) {
                break;
            }

            for (File file : files) {
                for (Migration<File> migration : dataMigrations) {
                    try {
                        if (migration.migrate(file)) {
                            logger.info(String.format("[%s] Migrated File \"%s\"", migration.getName(), file.getAbsolutePath()));

                            or = true;
                        }
                    } catch (Exception e) {
                        throw new MigrationFailedException(migration, getName(world), e);
                    }
                }
            }
        } while (or);
    }
}
