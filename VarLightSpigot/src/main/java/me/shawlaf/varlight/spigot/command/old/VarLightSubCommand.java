package me.shawlaf.varlight.spigot.command.old;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import me.shawlaf.command.ICommandAccess;
import me.shawlaf.command.brigadier.argument.EnumArgumentType;
import me.shawlaf.command.brigadier.argument.PlayerArgumentType;
import me.shawlaf.command.brigadier.argument.PositionArgumentType;
import me.shawlaf.command.brigadier.argument.WorldArgumentType;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.old.commands.arguments.BlockTypeArgumentType;
import me.shawlaf.varlight.spigot.command.old.commands.arguments.CollectionArgumentType;
import me.shawlaf.varlight.spigot.command.old.commands.arguments.ItemTypeArgumentType;
import me.shawlaf.varlight.util.pos.ChunkPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.ToIntFunction;

public abstract class VarLightSubCommand implements ICommandAccess<VarLightPlugin> {

    protected final VarLightPlugin _plugin;
    protected final VarLightCommand _rootCommand;
    private final String _name;

    public VarLightSubCommand(VarLightCommand rootCommand, String name) {
        _rootCommand = rootCommand;
        _plugin = rootCommand.getPlugin();
        _name = name;
    }

    protected static LiteralArgumentBuilder<CommandSender> literalArgument(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    protected static RequiredArgumentBuilder<CommandSender, Integer> integerArgument(String name) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer());
    }

    protected static RequiredArgumentBuilder<CommandSender, Integer> integerArgument(String name, int min) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer(min));
    }

    protected static RequiredArgumentBuilder<CommandSender, Integer> integerArgument(String name, int min, int max) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer(min, max));
    }

    protected static RequiredArgumentBuilder<CommandSender, Boolean> boolArgument(String name) {
        return RequiredArgumentBuilder.argument(name, BoolArgumentType.bool());
    }

    protected static RequiredArgumentBuilder<CommandSender, ICoordinates> positionArgument(String name) {
        return RequiredArgumentBuilder.argument(name, PositionArgumentType.position());
    }

    protected static <T> RequiredArgumentBuilder<CommandSender, Collection<T>> collectionArgument(String name, ArgumentType<T> argument) {
        return RequiredArgumentBuilder.argument(name, CollectionArgumentType.collection(argument));
    }

    protected static RequiredArgumentBuilder<CommandSender, World> worldArgument(String name) {
        return RequiredArgumentBuilder.argument(name, WorldArgumentType.world());
    }

    protected static RequiredArgumentBuilder<CommandSender, Player> playerArgument(String name) {
        return RequiredArgumentBuilder.argument(name, PlayerArgumentType.player());
    }

    protected static void suggestCoordinate(RequiredArgumentBuilder<CommandSender, Integer> coordinateArgument, ToIntFunction<Entity> coordinateSupplier) {
        coordinateArgument.suggests(((context, builder) -> {
            if (!(context.getSource() instanceof Entity)) {
                return builder.buildFuture();
            }

            builder.suggest(coordinateSupplier.applyAsInt((Entity) context.getSource()));

            return builder.buildFuture();
        }));
    }

    public abstract @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node);

    // region Util

    @Override
    public final @NotNull VarLightPlugin getPlugin() {
        return _plugin;
    }

    @Override
    public final @NotNull String getName() {
        return _name;
    }

    @Override
    public @NotNull String getSyntax() {
        return "";
    }

    @Override
    public @NotNull String getDescription() {
        return "";
    }


    @Override
    public final @NotNull String[] getAliases() {
        return new String[0];
    }

    public CommandNode<CommandSender> getNode() {
        return _rootCommand.getCommandDispatcher().getRoot().getChildren().iterator().next().getChild(getName());
    }

    @Override
    public @NotNull String getUsageString() {
        return getUsageString(Bukkit.getConsoleSender());
    }

    @Override
    public @NotNull String getUsageString(CommandSender commandSender) {
        return "/varlight " + _rootCommand.getCommandDispatcher().getSmartUsage(_rootCommand.getCommandDispatcher().getRoot().getChild(_rootCommand.getName()), commandSender).get(getNode());
    }

    protected RequiredArgumentBuilder<CommandSender, Material> item(String name) {
        return RequiredArgumentBuilder.argument(name, ItemTypeArgumentType.item(_plugin));
    }

    protected RequiredArgumentBuilder<CommandSender, Material> block(String name) {
        return RequiredArgumentBuilder.argument(name, BlockTypeArgumentType.block(_plugin));
    }

    protected <E extends Enum<E>> RequiredArgumentBuilder<CommandSender, E> enumArgument(String name, Class<E> enumType) {
        return RequiredArgumentBuilder.argument(name, EnumArgumentType.enumArgument(enumType));
    }

    @SuppressWarnings("unchecked")
    protected CompletableFuture<Void> createTickets(World world, Set<ChunkPosition> chunkCoords) {
        Runnable r = () -> {
            for (ChunkPosition chunkCoord : chunkCoords) {
                world.addPluginChunkTicket(chunkCoord.x(), chunkCoord.z(), _plugin);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            r.run();
            return CompletableFuture.completedFuture(null);
        } else {
            return _plugin.getApi().getSyncExecutor().submit(r, null);
        }
    }

    protected CompletableFuture<Void> releaseTickets(World world, Set<ChunkPosition> chunkCoords) {
        Runnable r = () -> {
            for (ChunkPosition chunkCoord : chunkCoords) {
                world.removePluginChunkTicket(chunkCoord.x(), chunkCoord.z(), _plugin);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            r.run();
            return CompletableFuture.completedFuture(null);
        } else {
            return _plugin.getApi().getSyncExecutor().submit(r, null);
        }
    }

    // endregion
}
