package me.shawlaf.varlight.spigot.command.old.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import me.shawlaf.varlight.spigot.permissions.PermissionNode;
import me.shawlaf.varlight.spigot.permissions.tree.VarLightPermissionTree;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.success;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

public class VarLightCommandStepSize extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_STEPSIZE = integerArgument("stepsize", 1, 15);

    public VarLightCommandStepSize(VarLightCommand command) {
        super(command, "stepsize");
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Edit the Step size when using " + plugin.getNmsAdapter().getKey(plugin.getApi().getLightUpdateItem()) + ".";
    }

    @Override
    public @Nullable PermissionNode getRequiredPermissionNode() {
        return VarLightPermissionTree.STEP_SIZE;
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.executes(this::getStepSize);
        node.then(ARG_STEPSIZE.executes(this::setStepSize));

        return node;
    }

    private int getStepSize(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "Only players may use this command!");

            return FAILURE;
        }

        Player player = (Player) context.getSource();
        success(this, player, String.format("You current stepsize is %d", plugin.getApi().getStepsizeManager().getStepSize(player)));

        return SUCCESS;
    }

    private int setStepSize(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "Only players may use this command!");

            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int newStepSize = context.getArgument(ARG_STEPSIZE.getName(), int.class);

        plugin.getApi().getStepsizeManager().setStepSize(player, newStepSize);

        success(this, player, String.format("Set your step size to %d", newStepSize));

        return SUCCESS;
    }
}
