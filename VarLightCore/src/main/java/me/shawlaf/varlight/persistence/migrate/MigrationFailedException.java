package me.shawlaf.varlight.persistence.migrate;

public class MigrationFailedException extends RuntimeException {

    public MigrationFailedException() {
    }

    public MigrationFailedException(Migration failedMigration, String worldName, Throwable cause) {
        this(String.format("Failed to migrate world \"%s\" during \"%s\": %s", worldName, failedMigration.getName(), cause.getMessage()), cause);
    }

    public MigrationFailedException(String message) {
        super(message);
    }

    public MigrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MigrationFailedException(Throwable cause) {
        super(cause);
    }

    public MigrationFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
