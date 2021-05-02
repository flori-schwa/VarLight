package me.shawlaf.varlight.persistence.migrate;

public interface Migration<M> {

    boolean migrate(M toMigrate) throws Exception;

    default String getName() {
        return getClass().getSimpleName();
    }

}
