package me.shawlaf.varlight.spigot.command.old.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import me.shawlaf.varlight.spigot.permissions.PermissionNode;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VarLightCommandPrompt extends VarLightSubCommand {
    public VarLightCommandPrompt(VarLightCommand rootCommand) {
        super(rootCommand, "prompt");
    }

    @Override
    public @Nullable PermissionNode getRequiredPermissionNode() {
        return null;
    }

    @Override
    public @NotNull String getDescription() {
        return "Used to confirm or cancel prompts";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        node.then(
                literalArgument("confirm").executes(c -> {
                    plugin.getApi().getChatPromptManager().confirmPrompt(c.getSource());

                    return VarLightCommand.SUCCESS;
                })
        );

        node.then(
                literalArgument("cancel").executes(c -> {
                    plugin.getApi().getChatPromptManager().cancelPrompt(c.getSource());

                    return VarLightCommand.SUCCESS;
                })
        );

        return node;
    }
}
