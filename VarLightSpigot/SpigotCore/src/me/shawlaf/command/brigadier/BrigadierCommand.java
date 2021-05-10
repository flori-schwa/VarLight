package me.shawlaf.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import me.shawlaf.command.AbstractCommand;
import me.shawlaf.command.ArgumentIterator;
import me.shawlaf.command.CommandSuggestions;
import me.shawlaf.command.exception.CommandException;
import me.shawlaf.command.result.CommandResult;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class BrigadierCommand<P extends Plugin> extends AbstractCommand<P> {

    protected final CommandDispatcher<CommandSender> commandDispatcher;
    private CommandNode<CommandSender> rootNode;
    private boolean built = false;

    public BrigadierCommand(P plugin, String name, boolean lateBuild) {
        super(plugin, name, false);

        this.commandDispatcher = new CommandDispatcher<>();

        if (!lateBuild) {
            build();
        }
    }

    public BrigadierCommand(P plugin, String name) {
        this(plugin, name, false);
    }

    public static CommandSyntaxException literal(String message) {
        return new SimpleCommandExceptionType(new LiteralMessage(message)).create();
    }

    public synchronized void build() {
        if (!built) {
            this.commandDispatcher.register(buildCommand(buildRootNode(getName())));
            this.rootNode = commandDispatcher.getRoot().getChildren().iterator().next();

            register();

            built = true;
        }
    }

    private String getFullInput(String[] args) {
        StringBuilder builder = new StringBuilder(getName());

        for (String arg : args) {
            builder.append(" ").append(arg);
        }

        return builder.toString();
    }

    protected abstract LiteralArgumentBuilder<CommandSender> buildCommand(LiteralArgumentBuilder<CommandSender> baseNode);

    protected LiteralArgumentBuilder<CommandSender> buildRootNode(String name) {
        return LiteralArgumentBuilder.<CommandSender>literal(name).requires(c -> {
            String required = getRequiredPermission();

            if (required == null || required.isEmpty()) {
                return true;
            }

            return c.hasPermission(required);
        });
    }

    protected final String getFullInput(ArgumentIterator args) {
        return getFullInput(args.getArguments());
    }

    @NotNull
    @Override
    public final String[] getAliases() {
        return new String[0];
    }

    @Override
    public @NotNull String getSyntax() {
        return "";
    }

    @Override
    public @NotNull String getUsageString(CommandSender commandSender) {
        Map<CommandNode<CommandSender>, String> smartUsage = commandDispatcher.getSmartUsage(commandDispatcher.getRoot(), commandSender);

        return "/" + smartUsage.get(smartUsage.keySet().iterator().next());
    }

    @Override
    public CommandResult execute(CommandSender sender, ArgumentIterator args) {
        ParseResults<CommandSender> parseResults = commandDispatcher.parse(getFullInput(args), sender);

        try {
            commandDispatcher.execute(parseResults);
        } catch (CommandSyntaxException e) {

            List<ParsedCommandNode<CommandSender>> parsedNodes = parseResults.getContext().getNodes();

            ParsedCommandNode<CommandSender> deepestNode = parsedNodes.get(0);

            for (int i = 1; i < parsedNodes.size(); i++) {
                if (parsedNodes.get(i).getRange().getStart() > deepestNode.getRange().getStart()) {
                    deepestNode = parsedNodes.get(i);
                }
            }

            String base = "/" + String.join(" ", commandDispatcher.getPath(deepestNode.getNode()));

            failure(e.getMessage()).finish(sender);

            sender.sendMessage("");

            info("Usage of " + base + ":", ChatColor.RED).finish(sender);

            Map<CommandNode<CommandSender>, String> usage = commandDispatcher.getSmartUsage(deepestNode.getNode(), sender);

            for (CommandNode<CommandSender> child : usage.keySet()) {
                sender.sendMessage(ChatColor.RED + base + " " + usage.get(child));
            }
        }

        return null;
    }

    @Override
    public void tabComplete(CommandSuggestions suggestions) {
        try {
            Suggestions result = commandDispatcher.getCompletionSuggestions(commandDispatcher.parse(getFullInput(suggestions.getArguments()), suggestions.getCommandSender())).get();

            suggestions.add(result.getList().stream().map(Suggestion::getText).collect(Collectors.toSet()));
        } catch (CommandException e) {
            // Ignore
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

}
