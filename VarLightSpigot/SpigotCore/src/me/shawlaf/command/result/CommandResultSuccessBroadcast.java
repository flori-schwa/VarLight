package me.shawlaf.command.result;

import me.shawlaf.command.ICommandAccess;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/**
 * Will broadcast the result of the command execution to everyone who has the required permission node
 */
public class CommandResultSuccessBroadcast extends CommandResult {

    private final String message;
    private final String node;

    public CommandResultSuccessBroadcast(ICommandAccess<?> command, String message) {
        this(command, message, command.getRequiredPermission());
    }

    public CommandResultSuccessBroadcast(ICommandAccess<?> command, String message, String node) {
        super(command);

        this.message = message;
        this.node = node;
    }

    @Override
    public void finish(CommandSender sender) {
        String msg = String.format("%s: %s", sender.getName(), getPrefixedMessage(message));
        String formatted = ChatColor.GRAY + "" + ChatColor.ITALIC + String.format("[%s]", msg);
        sender.sendMessage(getPrefixedMessage(message));

        Bukkit.getPluginManager().getPermissionSubscriptions(node).stream().filter(p -> p != sender && p instanceof CommandSender).forEach(p -> {
            if (p instanceof ConsoleCommandSender) {
                ((ConsoleCommandSender) p).sendMessage(msg);
            } else {
                ((CommandSender) p).sendMessage(formatted);
            }
        });
    }
}
