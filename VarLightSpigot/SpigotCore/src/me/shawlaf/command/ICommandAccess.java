package me.shawlaf.command;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ICommandAccess<P extends Plugin> {

    /**
     * @return the {@link Plugin} this command belongs to
     */
    @NotNull P getPlugin();

    /**
     * @return The name of the command
     */
    @NotNull String getName();

    /**
     * @return The Usage string in the format {@code /<name> <syntax>: <description>}
     */
    @NotNull String getUsageString();

    /**
     * @return The Usage String for the specified {@link CommandSender}
     */
    @NotNull String getUsageString(CommandSender commandSender);

    /**
     * @return The permission node required to run this command
     */
    @Nullable
    String getRequiredPermission();

    /**
     * @return The description of this command
     */
    @NotNull String getDescription();

    /**
     * @return The Syntax of this command. A list of arguments.
     */
    @NotNull String getSyntax();

    /**
     * @return A list of alternate command names that can be used instead of {@link AbstractCommand#getName()}
     */
    @NotNull String[] getAliases();
}
