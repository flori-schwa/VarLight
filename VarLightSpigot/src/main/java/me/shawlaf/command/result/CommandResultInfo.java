package me.shawlaf.command.result;

import me.shawlaf.command.ICommandAccess;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandResultInfo extends CommandResult {

    private final String message;
    private final ChatColor chatColor;

    public CommandResultInfo(ICommandAccess<?> command, String message, ChatColor chatColor) {
        super(command);

        this.message = message;
        this.chatColor = chatColor;
    }

    @Override
    public void finish(CommandSender sender) {
        sender.sendMessage(chatColor + getPrefixedMessage(message));
    }
}
