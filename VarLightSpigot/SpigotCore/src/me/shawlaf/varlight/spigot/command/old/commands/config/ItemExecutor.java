package me.shawlaf.varlight.spigot.command.old.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.*;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

public class ItemExecutor extends SubCommandExecutor {

    public ItemExecutor(VarLightSubCommand command) {
        super(command);
    }

    int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {
        info(
                command,
                context.getSource(),
                String.format(
                        "The current Light update item is \"%s\"",
                        plugin.getNmsAdapter().getKey(plugin.getApi().getLightUpdateItem())
                )
        );

        return SUCCESS;
    }

    int executeSet(CommandContext<CommandSender> context) throws CommandSyntaxException {
        Material item = context.getArgument("item", Material.class);

        if (plugin.getNmsAdapter().isIllegalLightUpdateItem(item)) {
            failure(command, context.getSource(), String.format("%s cannot be used as the varlight update item", plugin.getNmsAdapter().getKey(item)));

            return FAILURE;
        }

        plugin.getApi().setLightUpdateItem(item);
        successBroadcast(command, context.getSource(), String.format("Updated the Light update item to %s", plugin.getNmsAdapter().getKey(item)));

        return SUCCESS;
    }
}
