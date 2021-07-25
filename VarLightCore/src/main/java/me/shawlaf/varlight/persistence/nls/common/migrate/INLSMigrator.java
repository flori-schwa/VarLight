package me.shawlaf.varlight.persistence.nls.common.migrate;

import me.shawlaf.varlight.persistence.nls.common.NLSHeader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface INLSMigrator {

    NLSHeader migrateFile(File file) throws IOException;

    NLSHeader migrateData(InputStream data) throws IOException;

}
