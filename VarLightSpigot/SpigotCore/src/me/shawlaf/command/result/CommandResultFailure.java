package me.shawlaf.command.result;

import me.shawlaf.command.ICommandAccess;
import org.bukkit.ChatColor;

/**
 * Formats and sends the given message to the sender after completion
 */
public class CommandResultFailure extends CommandResultInfo {

    public CommandResultFailure(ICommandAccess<?> command, String message) {
        super(command, message, ChatColor.RED);
    }
}
