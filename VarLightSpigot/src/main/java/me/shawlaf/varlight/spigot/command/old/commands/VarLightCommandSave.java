package me.shawlaf.varlight.spigot.command.old.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import me.shawlaf.varlight.spigot.messages.VarLightMessages;
import me.shawlaf.varlight.spigot.permissions.PermissionNode;
import me.shawlaf.varlight.spigot.permissions.tree.VarLightPermissionTree;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public @Nullable PermissionNode getRequiredPermissionNode() {
        return VarLightPermissionTree.SAVE;
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

        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(player.getWorld())) == null) {
            failure(this, player, VarLightMessages.varLightNotActiveInWorld(player.getWorld()));

            return FAILURE;
        }

        cls.save(player, true);
        return SUCCESS;
    }

    private int saveAll(CommandContext<CommandSender> context) {
        for (ICustomLightStorage wlp : plugin.getApi().unsafe().getAllActiveVarLightWorlds()) {
            wlp.save(context.getSource(), true);
        }

        return SUCCESS;
    }

    private int saveExplicit(CommandContext<CommandSender> context) {
        World world = context.getArgument(ARG_WORLD.getName(), World.class);

        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(world)) == null) {
            failure(this, context.getSource(), VarLightMessages.varLightNotActiveInWorld(world));

            return FAILURE;
        }

        cls.save(context.getSource(), true);

        return SUCCESS;
    }

}
