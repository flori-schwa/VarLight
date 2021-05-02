package me.shawlaf.varlight.exception;

/**
 * Basically, a normal {@link java.io.IOException}, but unchecked
 */
public class VarLightIOException extends RuntimeException {

    public VarLightIOException() {
    }

    public VarLightIOException(String message) {
        super(message);
    }

    public VarLightIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public VarLightIOException(Throwable cause) {
        super(cause);
    }
}
