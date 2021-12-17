package me.shawlaf.varlight.spigot.command.old.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

public class ReclaimExecutor extends SubCommandExecutor {
    public ReclaimExecutor(VarLightSubCommand command) {
        super(command);
    }

    public int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {
        if (plugin.getVarLightConfig().isReclaimEnabled()) {
            info(command, context.getSource(), "Reclaim is enabled.", ChatColor.GREEN);
        } else {
            info(command, context.getSource(), "Reclaim is disabled.", ChatColor.RED);
        }

        return SUCCESS;
    }

    public int executeEnable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getVarLightConfig().setReclaim(true);

        successBroadcast(command, context.getSource(), "Enabled Reclaim.");

        return SUCCESS;
    }

    public int executeDisable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getVarLightConfig().setReclaim(false);

        successBroadcast(command, context.getSource(), "Disabled Reclaim.");

        return SUCCESS;
    }
}
