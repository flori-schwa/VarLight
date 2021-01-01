package me.shawlaf.varlight.exception;

public class LightUpdateFailedException extends RuntimeException {

    public LightUpdateFailedException() {
    }

    public LightUpdateFailedException(String message) {
        super(message);
    }

    public LightUpdateFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LightUpdateFailedException(Throwable cause) {
        super(cause);
    }

}
