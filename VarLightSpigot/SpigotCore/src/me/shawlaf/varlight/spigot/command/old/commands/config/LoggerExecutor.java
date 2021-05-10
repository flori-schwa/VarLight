package me.shawlaf.varlight.spigot.command.old.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

public class LoggerExecutor extends SubCommandExecutor {
    public LoggerExecutor(VarLightSubCommand command) {
        super(command);
    }

    public int executeVerboseGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        if (plugin.getVarLightConfig().isLogVerbose()) {
            info(command, context.getSource(), "Verbose logging is enabled.", ChatColor.GREEN);
        } else {
            info(command, context.getSource(), "Verbose logging is disabled.", ChatColor.RED);
        }

        return SUCCESS;
    }

    public int executeVerboseEnable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getVarLightConfig().setLogVerbose(true);

        successBroadcast(command, context.getSource(), "Enabled Verbose logging.");

        return SUCCESS;
    }

    public int executeVerboseDisable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getVarLightConfig().setLogVerbose(false);

        successBroadcast(command, context.getSource(), "Disabled Verbose logging.");

        return SUCCESS;
    }

    public int executeDebugGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        if (plugin.getVarLightConfig().isLogDebug()) {
            info(command, context.getSource(), "Debug logging is enabled.", ChatColor.GREEN);
        } else {
            info(command, context.getSource(), "Debug logging is disabled.", ChatColor.RED);
        }

        return SUCCESS;
    }

    public int executeDebugEnable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getVarLightConfig().setLogDebug(true);

        successBroadcast(command, context.getSource(), "Enabled Debug logging.");

        return SUCCESS;
    }

    public int executeDebugDisable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getVarLightConfig().setLogDebug(false);

        successBroadcast(command, context.getSource(), "Disabled Debug logging.");

        return SUCCESS;
    }
}
