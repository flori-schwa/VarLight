package me.shawlaf.command.result;

import me.shawlaf.command.ICommandAccess;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Represents an action that is performed after a command finishes Execution normally.
 */
public abstract class CommandResult {

    protected final ICommandAccess<?> command;

    public CommandResult(ICommandAccess<?> command) {
        this.command = command;
    }

    public static void info(ICommandAccess<?> command, CommandSender commandSender, String message, ChatColor chatColor) {
        new CommandResultInfo(command, message, chatColor).finish(commandSender);
    }

    public static void info(ICommandAccess<?> command, CommandSender commandSender, String message) {
        new CommandResultInfo(command, message, ChatColor.WHITE).finish(commandSender);
    }

    public static void failure(ICommandAccess<?> command, CommandSender commandSender, String message) {
        new CommandResultFailure(command, message).finish(commandSender);
    }

    public static void malformedSyntax(ICommandAccess<?> command, CommandSender commandSender) {
        new CommandResultMalformedSyntax(command).finish(commandSender);
    }

    public static void success(ICommandAccess<?> command, CommandSender commandSender, String message) {
        new CommandResultSuccess(command, message).finish(commandSender);
    }

    public static void successBroadcast(ICommandAccess<?> command, CommandSender commandSender, String message) {
        new CommandResultSuccessBroadcast(command, message).finish(commandSender);
    }

    public static void successBroadcast(ICommandAccess<?> command, CommandSender commandSender, String message, String node) {
        new CommandResultSuccessBroadcast(command, message, node).finish(commandSender);
    }

    public final ICommandAccess<?> getCommand() {
        return command;
    }

    public abstract void finish(CommandSender sender);

    // region Util methods

    protected void sendPrefixedMessage(CommandSender to, String message) {
        to.sendMessage(getPrefixedMessage(message));
    }

    protected String getPrefixedMessage(String message) {
        return String.format("[%s] %s", command.getPlugin().getName(), message);
    }

    // endregion

}
