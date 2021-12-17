package me.shawlaf.command;

import me.shawlaf.command.raytrace.PlayerRaytrace;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Collects suggestions for Tab-Completion of commands
 */
public class CommandSuggestions {

    private final CommandSender commandSender;
    private final String[] args;

    private List<String> suggestions = new ArrayList<>();

    /**
     * @param commandSender The source of tab completion
     * @param fullInput     The full input including the command name
     */
    public CommandSuggestions(CommandSender commandSender, String fullInput) {
        final String[] parts = fullInput.split(" ");
        final String[] args = new String[parts.length - 1];

        System.arraycopy(parts, 1, args, 0, args.length);

        this.commandSender = commandSender;
        this.args = args;
    }

    /**
     * @param commandSender The Source of tab completion
     * @param args          The arguments that have been typed already.
     */
    public CommandSuggestions(CommandSender commandSender, String[] args) {
        this.commandSender = commandSender;
        this.args = args;
    }

    /**
     * @return The Source of the tab completion
     */
    public CommandSender getCommandSender() {
        return commandSender;
    }

    /**
     * @return The arguments that have been typed already
     */
    public String[] getArguments() {
        return Arrays.copyOf(args, args.length);
    }

    /**
     * @return The list of suggestions
     */
    public List<String> getSuggestions() {
        return suggestions;
    }

    /**
     * @return The amount of arguments that have already been typed
     */
    public int getArgumentCount() {
        return args.length;
    }

    /**
     * @return The argument the {@link CommandSuggestions#getCommandSender()} is completing
     */
    public String getCursor() {
        return args[args.length - 1];
    }

    /**
     * Adds the {@code suggestion} to {@link CommandSuggestions#getSuggestions()} if {@code suggestion} starts with {@link CommandSuggestions#getCursor()}
     *
     * @param suggestion The suggestion
     * @return self
     */
    public CommandSuggestions add(String suggestion) {
        return add(suggestion, true);
    }

    /**
     * Adds the {@code suggestion} to {@link CommandSuggestions#getSuggestions()}.
     * If {@code checkArgument} is true, the {@code suggestion} must start with {@link CommandSuggestions#getCursor()} to be added to the List.
     *
     * @param suggestion The suggestion
     * @return self
     */
    public CommandSuggestions add(String suggestion, boolean checkArgument) {
        if (checkArgument && !suggestion.startsWith(getCursor())) {
            return this;
        }

        suggestions.add(suggestion);
        return this;
    }

    /**
     * Suggests a Block Position. If {@link CommandSuggestions#getCommandSender()} is a {@link Player} the coordinates of the Block the Player is looking at will be suggested
     *
     * @param completedCoordinates The amount of coordinated that have already been typed.
     * @return self
     */
    public CommandSuggestions suggestBlockPosition(int completedCoordinates) {
        return suggestBlockPosition(completedCoordinates, true);
    }

    /**
     * Adds all suggestions to {@link CommandSuggestions#getSuggestions()} if the {@code choice} starts with {@link CommandSuggestions#getCursor()}
     *
     * @param suggestions The suggestions to add
     * @return self
     */
    public CommandSuggestions add(Collection<String> suggestions) {
        for (String choice : suggestions) {
            add(choice);
        }

        return this;
    }

    /**
     * Adds all suggestions to {@link CommandSuggestions#getSuggestions()} if the {@code choice} starts with {@link CommandSuggestions#getCursor()}
     *
     * @param suggestions The suggestions to add
     * @return self
     */
    public CommandSuggestions add(String... suggestions) {
        for (String choice : suggestions) {
            add(choice);
        }

        return this;
    }

    /**
     * Suggests all online {@link Player}s whose {@link Player#getName()} begins with {@link CommandSuggestions#getCursor()}
     *
     * @return self
     */
    public CommandSuggestions suggestPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().startsWith(getCursor())) {
                add(player.getName());
            }
        }

        return this;
    }

    /**
     * Suggests a Block Position. If {@link CommandSuggestions#getCommandSender()} is a {@link Player} the coordinates of the Block the Player is looking at will be suggested
     *
     * @param completedCoordinates The amount of coordinated that have already been typed.
     * @param checkArgument        Whether the {@code checkArgument} flag should be set for {@link CommandSuggestions#add(String, boolean)}
     * @return self
     */
    public CommandSuggestions suggestBlockPosition(int completedCoordinates, boolean checkArgument) {
        if (!(commandSender instanceof Player)) {
            return this;
        }

        Player player = (Player) commandSender;

        final int[] coords = getCoordinatesLookingAt(player);

        if (coords.length == 0) {
            return this;
        }

        final int[] toSuggest = new int[3 - completedCoordinates];

        System.arraycopy(coords, completedCoordinates, toSuggest, 0, toSuggest.length);

        for (int i = 0; i < toSuggest.length; i++) {
            StringBuilder builder = new StringBuilder();

            for (int j = 0; j <= i; j++) {
                builder.append(toSuggest[j]);
                builder.append(" ");
            }

            add(builder.toString().trim(), checkArgument);
        }

        return this;
    }

    private int[] getCoordinatesLookingAt(Player player) {
        Block targetBlock = PlayerRaytrace.getTargetBlock(player, 10);

        if (targetBlock == null) {
            return new int[0];
        }

        return new int[]{
                targetBlock.getX(),
                targetBlock.getY(),
                targetBlock.getZ()
        };
    }
}