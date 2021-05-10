package me.shawlaf.command.result;

import me.shawlaf.command.ICommandAccess;
import org.bukkit.command.CommandSender;

public class CommandResultMalformedSyntax extends CommandResult {

    public CommandResultMalformedSyntax(ICommandAccess<?> command) {
        super(command);
    }

    @Override
    public void finish(CommandSender sender) {
        sendPrefixedMessage(sender, command.getUsageString());
    }
}
