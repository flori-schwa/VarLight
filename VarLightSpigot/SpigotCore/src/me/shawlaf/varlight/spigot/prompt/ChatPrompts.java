package me.shawlaf.varlight.spigot.prompt;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.Ticks;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import me.shawlaf.varlight.spigot.util.collections.EntityToObjectMap;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ChatPrompts implements IPluginLifeCycleOperations {

    private Prompt consolePrompt;
    private final EntityToObjectMap<Prompt> activePrompts = new EntityToObjectMap<>();
    private final VarLightPlugin plugin;

    public ChatPrompts(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onDisable() {
        activePrompts.clear();
        consolePrompt = null;
    }

    private Prompt getCurrentActivePrompt(CommandSender sender) {
        Prompt prompt;

        if (sender instanceof Entity) {
            prompt = activePrompts.get(((Entity) sender));

            if (prompt.isTerminated()) {
                activePrompts.remove(((Entity) sender));
                prompt = null;
            }
        } else if (sender instanceof ConsoleCommandSender) {
            prompt = consolePrompt;

            if (prompt.isTerminated()) {
                consolePrompt = prompt = null;
            }
        } else {
            sender.sendMessage(String.format("%s command sender not supported", sender.getClass().getName()));
            return null;
        }

        return prompt;
    }

    public boolean runPrompt(@NotNull CommandSender source, @NotNull BaseComponent[] message, @NotNull Runnable onConfirm, @NotNull Ticks timeout) {
        Prompt prompt = getCurrentActivePrompt(source);

        if (prompt != null) {
            source.sendMessage(ChatColor.RED + "You already have an prompt pending.");
            return false;
        }

        prompt = new Prompt(plugin, message, onConfirm);

        prompt.startWithTimeout(source, timeout);

        if (source instanceof Entity) {
            source.spigot().sendMessage(
                    new ComponentBuilder("[CONFIRM]")
                            .bold(true)
                            .color(net.md_5.bungee.api.ChatColor.GREEN)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/varlight prompt confirm"))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here to accept the prompt").create()))
                            .append(" ")
                            .append("[CANCEL]")
                            .bold(true)
                            .color(net.md_5.bungee.api.ChatColor.RED)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/varlight prompt cancel"))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here to cancel the prompt").create()))
                            .create()
            );
        } else {
            source.sendMessage(ChatColor.RED + "Type /varlight prompt confirm/cancel to confirm/cancel.");
        }

        source.sendMessage(ChatColor.RED + "Prompt will automatically cancel in " + timeout.toReadable());

        if (source instanceof Entity) {
            activePrompts.put(((Entity) source), prompt);
        } else {
            consolePrompt = prompt;
        }

        return true;
    }

    private Prompt checkActivePrompt(CommandSender source) {
        Prompt prompt = getCurrentActivePrompt(source);

        if (prompt == null) {
            source.sendMessage(ChatColor.RED + "You currently don't have a prompt pending.");
            return null;
        }

        return prompt;
    }

    public void confirmPrompt(CommandSender source) {
        Optional.ofNullable(checkActivePrompt(source)).ifPresent(Prompt::confirm);
    }

    public void cancelPrompt(CommandSender source) {
        Optional.ofNullable(checkActivePrompt(source)).ifPresent(Prompt::cancel);
    }
}
