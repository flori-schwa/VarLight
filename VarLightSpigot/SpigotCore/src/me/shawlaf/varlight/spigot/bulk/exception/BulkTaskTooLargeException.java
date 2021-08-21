package me.shawlaf.varlight.spigot.bulk.exception;

import lombok.Getter;

public class BulkTaskTooLargeException extends Exception {

    @Getter
    private final int chunkLimit;
    @Getter
    private final int nChunksTryingToModify;

    public BulkTaskTooLargeException(int chunkLimit, int nChunksTryingToModify) {
        this.chunkLimit = chunkLimit;
        this.nChunksTryingToModify = nChunksTryingToModify;
    }
}
