package me.shawlaf.varlight.spigot.bulk.exception;

public class BulkTaskTooLargeException extends Exception {

    private final int chunkLimit;
    private final long nChunksTryingToModify;

    public BulkTaskTooLargeException(int chunkLimit, long nChunksTryingToModify) {
        this.chunkLimit = chunkLimit;
        this.nChunksTryingToModify = nChunksTryingToModify;
    }

    public int getChunkLimit() {
        return chunkLimit;
    }

    public long getAmountOfChunksTryingToModify() {
        return nChunksTryingToModify;
    }
}
