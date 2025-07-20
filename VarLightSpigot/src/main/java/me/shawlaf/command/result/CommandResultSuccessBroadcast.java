package me.shawlaf.command.result;


import me.shawlaf.command.ICommandAccess;
import me.shawlaf.varlight.spigot.permissions.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/**
 * Will broadcast the result of the command execution to everyone who has the required permission node
 */
public class CommandResultSuccessBroadcast extends CommandResult {

    private final String message;
    private final PermissionNode node;

    public CommandResultSuccessBroadcast(ICommandAccess<?> command, String message) {
        this(command, message, command.getRequiredPermissionNode());
    }

    public CommandResultSuccessBroadcast(ICommandAccess<?> command, String message, PermissionNode node) {
        super(command);

        this.message = message;
        this.node = node;
    }

    @Override
    public void finish(CommandSender sender) {
        String msg = String.format("%s: %s", sender.getName(), getPrefixedMessage(message));
        String formatted = ChatColor.GRAY + "" + ChatColor.ITALIC + String.format("[%s]", msg);
        sender.sendMessage(getPrefixedMessage(message));

        Bukkit.getPluginManager()
                .getPermissionSubscriptions(node == null ? "" : node.getFullName()).stream()
                .filter(p -> p != sender && p instanceof CommandSender)
                .map(CommandSender.class::cast)
                .forEach(recipient -> {
                    if (recipient instanceof ConsoleCommandSender) {
                        recipient.sendMessage(msg);
                    } else {
                        recipient.sendMessage(formatted);
                    }
                });
    }
}
