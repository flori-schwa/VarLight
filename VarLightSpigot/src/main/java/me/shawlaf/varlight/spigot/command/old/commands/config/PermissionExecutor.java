package me.shawlaf.varlight.spigot.command.old.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

public class PermissionExecutor extends SubCommandExecutor {

    public PermissionExecutor(VarLightSubCommand command) {
        super(command);
    }

    int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        boolean doCheckPermission = plugin.getVarLightConfig().isCheckingPermission();

        if (doCheckPermission) {
            info(command, context.getSource(), String.format("Only players with the \"varlight.use\" permission node may %s to update Light sources", plugin.getNmsAdapter().getKey(plugin.getApi().getLightUpdateItem())));
        } else {
            info(command, context.getSource(), "There is currently no permisison requirement to use plugin features.");
        }

        return SUCCESS;
    }

    int executeSet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        boolean newValue = context.getArgument("value", boolean.class);

        if (newValue == plugin.getVarLightConfig().isCheckingPermission()) {
            info(command, context.getSource(), "Nothing changed.");
        } else {
            plugin.getVarLightConfig().setCheckPermission(newValue);

            if (newValue) {
                successBroadcast(command, context.getSource(), String.format("Enabled permission checking, only players with the \"varlight.use\" permission node may use %s to update Light sources", plugin.getNmsAdapter().getKey(plugin.getApi().getLightUpdateItem())));
            } else {
                successBroadcast(command, context.getSource(), String.format("Disabled permission checking, all players may use %s to update Light sources", plugin.getNmsAdapter().getKey(plugin.getApi().getLightUpdateItem())));
            }
        }


        return SUCCESS;
    }
}
