package me.shawlaf.varlight.spigot.command.old.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.adapters.WorldEditAdapter;
import me.shawlaf.varlight.spigot.async.Ticks;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import me.shawlaf.varlight.spigot.command.old.util.IPlayerSelection;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.spigot.util.VarLightPermissions;
import me.shawlaf.varlight.util.pos.IntPosition;
import me.shawlaf.varlight.util.pos.RegionIterator;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

@ExtensionMethod({
        IntPositionExtension.class
})
public class VarLightCommandClear extends VarLightSubCommand implements IPlayerSelection {

    public static final String ARG_NAME_POS1 = "position 1";
    public static final String ARG_NAME_POS2 = "position 2";

    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_1 = positionArgument(ARG_NAME_POS1);
    private static final RequiredArgumentBuilder<CommandSender, ICoordinates> ARG_POS_2 = positionArgument(ARG_NAME_POS2);

    private WorldEditAdapter worldEditUtil;

    public VarLightCommandClear(VarLightCommand rootCommand) {
        super(rootCommand, "clear");

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            this.worldEditUtil = new WorldEditAdapter();
        }
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return VarLightPermissions.VARLIGHT_CLEAR_PERMISSION;
    }

    @Override
    public @NotNull String getDescription() {
        return "Remove Custom Light sources in the specified area";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        if (node.getRequirement() != null) {
            Predicate<CommandSender> requirement = node.getRequirement();
            requirement = requirement.and(cs -> cs instanceof LivingEntity);
            node.requires(requirement);
        } else {
            node.requires(cs -> cs instanceof LivingEntity);
        }

        node.then(
                ARG_POS_1.then(
                        ARG_POS_2.executes(this::clearExplicitSelection)
                )
        );

        if (this.worldEditUtil != null) {
            node.executes(this::clearImplicitSelection);
        }

        return node;
    }

    private int clearImplicitSelection(CommandContext<CommandSender> context) throws CommandSyntaxException {
        return startPrompt(((LivingEntity) context.getSource()), getSelection(context, true));
    }

    private int clearExplicitSelection(CommandContext<CommandSender> context) throws CommandSyntaxException {
        return startPrompt(((LivingEntity) context.getSource()), getSelection(context, false));
    }

    private int startPrompt(LivingEntity source, Location[] selection) {
        World world = source.getWorld();
        RegionIterator targetArea = new RegionIterator(selection[0].toIntPosition(), selection[1].toIntPosition());

        plugin.getApi().getChatPromptManager().runPrompt(
                source,
                new ComponentBuilder("[VarLight] Are you sure, you want to ")
                        .append("delete all Light sources in your selected area? (" + targetArea.getSize() + " total blocks, " + targetArea.iterateChunks().getSize() + " chunks)").color(ChatColor.RED)
                        .append("This action cannot be undone.").color(ChatColor.RED).underlined(true).create(),
                () -> clear(source, world, selection),
                Ticks.of(1, TimeUnit.MINUTES)
        );

        return SUCCESS;
    }

    private void clear(LivingEntity source, World world, Location[] selection) {
        if (Bukkit.isPrimaryThread()) {
            // Ensure this is running on a different thread
            plugin.getApi().getAsyncExecutor().submit(() -> clear(source, world, selection));
            return;
        }

        IntPosition a = selection[0].toIntPosition();
        IntPosition b = selection[1].toIntPosition();

        info(this, source, "Clearing Custom Light data in selection...");

        plugin.getApi().runBulkClear(source.getWorld(), source, a, b).join().finish(source);
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
}
