package me.shawlaf.command.result;

import me.shawlaf.command.ICommandAccess;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Formats and sends the given message to the sender after completion
 */
public class CommandResultSuccess extends CommandResultInfo {
    public CommandResultSuccess(ICommandAccess<?> command, String message) {
        super(command, message, ChatColor.GREEN);
    }
}
