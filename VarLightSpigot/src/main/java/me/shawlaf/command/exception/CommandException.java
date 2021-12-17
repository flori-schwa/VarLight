package me.shawlaf.command.exception;

import me.shawlaf.command.ArgumentIterator;
import me.shawlaf.command.AbstractCommand;
import org.bukkit.command.CommandSender;

/**
 * This throwable class represents a {@link RuntimeException} involving the execution of {@link AbstractCommand}'s
 * If an Exception of this Type is thrown during the execution of a {@link AbstractCommand} ({@link AbstractCommand#execute(CommandSender, ArgumentIterator)}),
 * the execution will stop and the {@link Exception#getMessage()} will be printed to the {@link AbstractCommand}'s {@link CommandSender}.
 * <p>
 * There are two Types of {@link CommandException}s: severe and mild.
 * <ul>
 *     <li>Mild {@link CommandException}s will only send {@link Exception#getMessage()} to the {@link CommandSender}.</li>
 *     <li>Severe {@link CommandException}s will also log the Stacktrace.</li>
 * </ul>
 */
public class CommandException extends RuntimeException {

    private final boolean severe;

    private CommandException(String message, boolean severe) {
        super(message);

        this.severe = severe;
    }

    private CommandException(String message, Throwable cause, boolean severe) {
        super(message, cause);

        this.severe = severe;
    }

    private CommandException(Throwable cause, boolean severe) {
        super(cause);

        this.severe = severe;
    }

    public static CommandException severeException(String message) {
        return new CommandException(message, true);
    }

    public static CommandException severeException(String message, Throwable cause) {
        return new CommandException(message, cause, true);
    }

    public static CommandException severeException(Throwable cause) {
        return new CommandException(cause, true);
    }

    public static CommandException mildException(String message) {
        return new CommandException(message, false);
    }

    public static CommandException mildException(String message, Throwable cause) {
        return new CommandException(message, cause, false);
    }

    public static CommandException mildException(Throwable cause) {
        return new CommandException(cause, false);
    }

    /**
     * @return Whether or not the Stacktrace should be logged.
     */
    public boolean isSevere() {
        return severe;
    }
}
