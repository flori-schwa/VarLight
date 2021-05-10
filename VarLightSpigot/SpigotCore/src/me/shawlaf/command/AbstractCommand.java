package me.shawlaf.command;

import me.shawlaf.command.exception.CommandException;
import me.shawlaf.command.result.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a Custom Command. The command will register itself during Runtime. no entries are required in config.yml
 */
public abstract class AbstractCommand<P extends Plugin> implements ICommandAccess<P> {

    private static Field simpleCommandMapField;
    private static Constructor<PluginCommand> pluginCommandConstructor;

    static {
        try {
            simpleCommandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
            pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);

            simpleCommandMapField.setAccessible(true);
            pluginCommandConstructor.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    protected final P plugin;
    private final String name;

    public AbstractCommand(P plugin, String name) {
        this(plugin, name, true);
    }

    public AbstractCommand(P plugin, String name, boolean registerNow) {
        this.plugin = plugin;
        this.name = name;

        if (registerNow) {
            register();
        }
    }

    protected void register() {
        try {
            final PluginCommand pluginCommand = pluginCommandConstructor.newInstance(getName(), plugin);

            pluginCommand.setDescription(getDescription());
            pluginCommand.setAliases(Stream.of(getAliases()).collect(Collectors.toList()));
            pluginCommand.setPermission(getRequiredPermission());
            pluginCommand.setUsage(getUsageString());

            pluginCommand.setExecutor((sender, cmd, label, args) -> {
                final ArgumentIterator argumentIterator = new ArgumentIterator(args);

                try {
                    Optional.ofNullable(AbstractCommand.this.execute(sender, argumentIterator)).ifPresent(r -> r.finish(sender));

                    return true;
                } catch (CommandException commandException) {
                    failure(commandException.getMessage()).finish(sender);

                    if (commandException.isSevere()) {
                        commandException.printStackTrace();
                    }

                    return true;
                }
            });

            pluginCommand.setTabCompleter((sender, cmd, label, args) -> {
                CommandSuggestions commandSuggestions = new CommandSuggestions(sender, args);
                tabComplete(commandSuggestions);

                return commandSuggestions.getSuggestions();
            });

            ((CommandMap) simpleCommandMapField.get(Bukkit.getPluginManager())).register(plugin.getName(), pluginCommand);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }


    @NotNull
    @Override
    public final P getPlugin() {
        return plugin;
    }


    @NotNull
    @Override
    public String getUsageString() {
        return getUsageString(Bukkit.getConsoleSender());
    }

    @Override
    public @NotNull String getUsageString(CommandSender commandSender) {
        StringBuilder builder = new StringBuilder("/");

        builder.append(getName());

        if (!getSyntax().isEmpty()) {
            builder.append(" ").append(getSyntax());
        }

        builder.append(":  ").append(getDescription());

        return builder.toString();
    }

    @NotNull
    @Override
    public final String getName() {
        return name;
    }

    @Override
    @Nullable
    public String getRequiredPermission() {
        return null;
    }


    @NotNull
    @Override
    public String getDescription() {
        return "";
    }


    @NotNull
    @Override
    public String getSyntax() {
        return "";
    }


    @NotNull
    @Override
    public String[] getAliases() {
        return new String[0];
    }

    /**
     * Executes the command, returning the result
     *
     * @param sender The Source of the command
     * @param args   The arguments given to the commmand
     * @return The {@link CommandResult} of the command. May be null
     */
    public abstract CommandResult execute(CommandSender sender, ArgumentIterator args);

    /**
     * @param suggestions Fills the specified {@link CommandSuggestions} with suggestions to Tab-Complete
     */
    public void tabComplete(CommandSuggestions suggestions) {

    }

    // region Util methods

    protected void sendPrefixedMessage(CommandSender to, String message) {
        to.sendMessage(getPrefixedMessage(message));
    }

    protected String getPrefixedMessage(String message) {
        return String.format("[%s] %s", plugin.getName(), message);
    }

    /**
     * Asserts that the specified {@link CommandSender} is an instance of {@code <S>}.
     *
     * @param commandSender The Source of the command
     * @param sClass        The class the sender should be an instance of
     * @param <S>           The Type the sender should be
     * @return The {@code commandSender} casted to {@code <S>}
     * @throws CommandException if the {@code commandSender} cannot be cast to {@code <S>}
     */
    protected <S extends CommandSender> S assertSenderInstanceOf(CommandSender commandSender, Class<S> sClass) {
        return assertSenderInstanceOf(commandSender, sClass, "You must be a " + sClass.getSimpleName() + " to use this command!");
    }

    /**
     * Asserts that the specified {@link CommandSender} is an instance of {@code <S>}.
     *
     * @param commandSender The Source of the command
     * @param sClass        The class the sender should be an instance of
     * @param message       The message to display if the specified {@link CommandSender} is not an instance of {@code <S>}
     * @param <S>           The Type the sender should be
     * @return The {@code commandSender} casted to {@code <S>}
     * @throws CommandException if the {@code commandSender} cannot be cast to {@code <S>}
     */
    protected <S extends CommandSender> S assertSenderInstanceOf(CommandSender commandSender, Class<S> sClass, String message) {
        if (sClass.isAssignableFrom(commandSender.getClass())) {
            return sClass.cast(commandSender);
        }

        throw CommandException.mildException(message);
    }

    protected CommandResult info(String message, ChatColor chatColor) {
        return new CommandResultInfo(this, message, chatColor);
    }

    protected CommandResult info(String message) {
        return info(message, ChatColor.WHITE);
    }

    protected CommandResult failure(String message) {
        return new CommandResultFailure(this, message);
    }

    protected CommandResult success(String message) {
        return new CommandResultSuccess(this, message);
    }

    protected CommandResult successBroadcast(String message, String permissionNode) {
        return new CommandResultSuccessBroadcast(this, message, permissionNode);
    }

    protected CommandResult successBroadcast(String message) {
        return successBroadcast(message, getRequiredPermission());
    }

    protected CommandResult malformedSyntax() {
        return new CommandResultMalformedSyntax(this);
    }

    // endregion
}
