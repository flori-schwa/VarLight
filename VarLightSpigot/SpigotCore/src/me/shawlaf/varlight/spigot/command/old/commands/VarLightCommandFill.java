package me.shawlaf.varlight.spigot.command.old.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.adapters.WorldEditAdapter;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import me.shawlaf.varlight.spigot.command.old.commands.arguments.BlockTypeArgumentType;
import me.shawlaf.varlight.spigot.command.old.util.IPlayerSelection;
import me.shawlaf.varlight.spigot.permissions.PermissioneNode;
import me.shawlaf.varlight.spigot.permissions.VarLightPermissionTree;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

@SuppressWarnings("DuplicatedCode")
@ExtensionMethod({
        IntPositionExtension.class
})
public class VarLightCommandFill extends VarLightSubCommand implements IPlayerSelection {

    public static final String ARG_NAME_POS1 = "position 1";
    public static final String ARG_NAME_POS2 = "position 2";
    public static final String ARG_NAME_LIGHT_LEVEL = "light level";
    public static final String ARG_NAME_POSITIVE_FILTER = "posFilter";
    public static final String ARG_NAME_NEGATIVE_FILTER = "negFilter";

    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_1 = positionArgument(ARG_NAME_POS1);
    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_2 = positionArgument(ARG_NAME_POS2);

    private WorldEditAdapter worldEditUtil;

    public VarLightCommandFill(VarLightCommand command) {
        super(command, "fill");

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            this.worldEditUtil = new WorldEditAdapter();
        }
    }

    @Override
    public @Nullable PermissioneNode getRequiredPermissionNode() {
        return VarLightPermissionTree.instance().admin.modify.modifyBase;
    }

    @Override
    public @NotNull String getDescription() {
        return "Fills a large area with custom light sources.";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(
                ARG_POS_1.then(
                        ARG_POS_2.then(
                                integerArgument(ARG_NAME_LIGHT_LEVEL, 0, 15)
                                        .executes(c -> fillNoFilter(c, false))
                                        .then(
                                                literalArgument("include")
                                                        .then(
                                                                collectionArgument(ARG_NAME_POSITIVE_FILTER, BlockTypeArgumentType.block(plugin))
                                                                        .executes(c -> fillPosFilter(c, false))
                                                        )
                                        )
                                        .then(
                                                literalArgument("exclude")
                                                        .then(
                                                                collectionArgument(ARG_NAME_NEGATIVE_FILTER, BlockTypeArgumentType.block(plugin))
                                                                        .executes(c -> fillNegFilter(c, false))
                                                        )
                                        )
                        )
                )
        );

        if (this.worldEditUtil != null) {
            node.then(
                    integerArgument(ARG_NAME_LIGHT_LEVEL, 0, 15)
                            .executes(c -> fillNoFilter(c, true))
                            .then(
                                    literalArgument("include")
                                            .then(
                                                    collectionArgument(ARG_NAME_POSITIVE_FILTER, BlockTypeArgumentType.block(plugin))
                                                            .executes(c -> fillPosFilter(c, true))
                                            )
                            )
                            .then(
                                    literalArgument("exclude")
                                            .then(
                                                    collectionArgument(ARG_NAME_NEGATIVE_FILTER, BlockTypeArgumentType.block(plugin))
                                                            .executes(c -> fillNegFilter(c, true))
                                            )
                            )
            );
        }

        return node;
    }

    @Override
    public RequiredArgumentBuilder<CommandSender, ICoordinates> getPositionArgumentA() {
        return ARG_POS_1;
    }

    @Override
    public RequiredArgumentBuilder<CommandSender, ICoordinates> getPositionArgumentB() {
        return ARG_POS_2;
    }

    @Override
    public WorldEditAdapter getWorldEditAdapter() {
        return worldEditUtil;
    }

    private int fillNoFilter(CommandContext<CommandSender> context, boolean worldedit) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location[] selection = getSelection(context, worldedit);

        if (selection == null) {
            failure(this, context.getSource(), "You do not have a WorldEdit selection in that world");
            return FAILURE;
        }

        int lightLevel = context.getArgument(ARG_NAME_LIGHT_LEVEL, int.class);

        return fill((Player) context.getSource(), selection[0], selection[1], lightLevel, x -> true);
    }

    private int fillPosFilter(CommandContext<CommandSender> context, boolean worldedit) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location[] selection = getSelection(context, worldedit);

        if (selection == null) {
            failure(this, context.getSource(), "You do not have a WorldEdit selection in that world");
            return FAILURE;
        }

        int lightLevel = context.getArgument(ARG_NAME_LIGHT_LEVEL, int.class);

        Collection<Material> positiveFilter = context.getArgument(ARG_NAME_POSITIVE_FILTER, Collection.class);

        return fill((Player) context.getSource(), selection[0], selection[1], lightLevel, block -> positiveFilter.contains(block.getType()));
    }

    private int fillNegFilter(CommandContext<CommandSender> context, boolean worldedit) throws CommandSyntaxException {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command");
        }

        Location[] selection = getSelection(context, worldedit);

        if (selection == null) {
            failure(this, context.getSource(), "You do not have a WorldEdit selection in that world");
            return FAILURE;
        }

        int lightLevel = context.getArgument(ARG_NAME_LIGHT_LEVEL, int.class);

        Collection<Material> negativeFilter = context.getArgument(ARG_NAME_NEGATIVE_FILTER, Collection.class);

        return fill((Player) context.getSource(), selection[0], selection[1], lightLevel, block -> !negativeFilter.contains(block.getType()));
    }

    private int fill(Player source, Location pos1, Location pos2, int lightLevel, Predicate<Block> filter) {
        plugin.getApi().getAsyncExecutor().submit(
                () -> {
                    plugin.getApi().runBulkFill(source.getWorld(), source, pos1.toIntPosition(), pos2.toIntPosition(), lightLevel, filter).join().finish(source);
                }
        );

        return SUCCESS;
    }
}
