package me.shawlaf.varlight.spigot.prompt;

import lombok.Getter;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.Ticks;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Prompt {

    private static final AtomicInteger ID = new AtomicInteger(0);

    @Getter
    private final int id;

    private boolean timeout = false;
    private boolean completed = false;
    private boolean cancelled = false;

    @Getter
    private final @NotNull VarLightPlugin plugin;

    private final @NotNull BaseComponent[] bungeeMessage;

    private final @NotNull Runnable onConfirm;

    @Nullable
    private BukkitTask timeoutTask;

    public Prompt(@NotNull VarLightPlugin plugin, @NotNull BaseComponent[] message, @NotNull Runnable onConfirm) {
        Objects.requireNonNull(plugin, "Plugin may not be null");
        Objects.requireNonNull(message, "Message may not be null");
        Objects.requireNonNull(onConfirm, "OnConfirm Runnable may not be null");

        this.id = ID.getAndIncrement();

        this.plugin = plugin;
        this.bungeeMessage = message;
        this.onConfirm = onConfirm;
    }

    public void startWithTimeout(CommandSender source, Ticks timeoutTicks) {
        synchronized (this) {
            source.spigot().sendMessage(bungeeMessage);

            this.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
               synchronized (Prompt.this) {
                   if (!completed && !cancelled) {
                       this.timeout = true;
                       source.sendMessage(ChatColor.RED + "Your Chat prompt has timed out.");
                   }
               }
            }, timeoutTicks.ticks);
        }
    }

    public void confirm() {
        synchronized (this) {
            if (isTerminated()) {
                return;
            }

            if (this.timeoutTask != null) {
                this.timeoutTask.cancel();
            }

            this.completed = true;
            this.onConfirm.run();
        }
    }

    public void cancel() {
        synchronized (this) {
            if (isTerminated()) {
                return;
            }

            if (this.timeoutTask != null) {
                this.timeoutTask.cancel();
            }

            this.cancelled = true;
        }
    }

    public boolean isTerminated() {
        return this.timeout || this.completed || this.cancelled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
