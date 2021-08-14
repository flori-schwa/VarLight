package me.shawlaf.varlight.spigot.command.old.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

public class VarLightCommandSave extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, World> ARG_WORLD = worldArgument("world");

    public VarLightCommandSave(VarLightCommand command) {
        super(command, "save");
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Save all custom light sources in the current world, the specified world or all worlds.";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.save";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        node.executes(this::saveImplicit);
        node.then(LiteralArgumentBuilder.<CommandSender>literal("all").executes(this::saveAll));
        node.then(ARG_WORLD.executes(this::saveExplicit));

        return node;
    }

    private int saveImplicit(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "Only players may use this command!");

            return FAILURE;
        }

        Player player = (Player) context.getSource();

        try {
            plugin.getApi().unsafe().requireVarLightEnabled(player.getWorld()).save(player, true);

            return SUCCESS;
        } catch (VarLightNotActiveException e) {
            failure(this, player, e.getMessage());

            return FAILURE;
        }


    }

    private int saveAll(CommandContext<CommandSender> context) {
        for (ICustomLightStorage wlp : plugin.getApi().getAllActiveVarLightWorlds()) {
            wlp.save(context.getSource(), true);
        }

        return SUCCESS;
    }

    private int saveExplicit(CommandContext<CommandSender> context) {
        World world = context.getArgument(ARG_WORLD.getName(), World.class);

        try {
            plugin.getApi().unsafe().requireVarLightEnabled(world).save(context.getSource(), true);

            return SUCCESS;
        } catch (VarLightNotActiveException e) {
            failure(this, context.getSource(), String.format("Varlight is not active in world \"%s\"", world.getName()));

            return FAILURE;
        }
    }

}
