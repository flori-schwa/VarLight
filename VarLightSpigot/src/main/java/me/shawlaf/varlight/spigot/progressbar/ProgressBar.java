package me.shawlaf.varlight.spigot.progressbar;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.async.Ticks;
import me.shawlaf.varlight.util.StringUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressBar implements AutoCloseable {

    private static final AtomicInteger ID_TICKER = new AtomicInteger(0);

    public static final ProgressBar NULL_PROGRESS_BAR = new ProgressBar(null, "", 1) {

        @Override
        public void subscribeAll(Collection<CommandSender> subscribers) {
            // nop
        }

        @Override
        public void subscribe(CommandSender sender) {
            // nop
        }

        @Override
        public void advanceTo(long n) {
            // nop
        }

        @Override
        public void step() {
            // nop
        }

        @Override
        public void step(long n) {
            // nop
        }

        @Override
        public void close() {
            // nop
        }
    };


    private final VarLightPlugin plugin;
    private final int id;
    private final String name;
    private final long totalUnits;
    private long completedUnits = 0;

    private BukkitTask keepOnScreenTask = null;

    private int lastPercent = 0;
    private Set<CommandSender> subscribers = new HashSet<>();

    private static BaseComponent[] generateFormattedProgressBar(String name, int percentage) {
        int fill = percentage / 2;

        /*
            Inspiration for Design:
                https://www.spigotmc.org/threads/progress-bars-and-percentages.276020/
         */

        return new ComponentBuilder(name)
                .color(ChatColor.GREEN)
                .append(" [")
                    .color(ChatColor.DARK_GRAY)
                .append(StringUtil.repeat("|", Math.max(0, fill)))
                    .color(ChatColor.GREEN)
                .append(StringUtil.repeat("|", Math.max(0, 50 - fill)))
                    .color(ChatColor.GRAY)
                .append("]")
                    .color(ChatColor.DARK_GRAY)
                .append(" (" + percentage + "%)")
                    .color(ChatColor.GREEN)
            .create();
    }

    private static String generateProgressBar(int percentage) {
        StringBuilder builder = new StringBuilder("[");
        int fill = percentage / 2;

        builder.append(StringUtil.repeat("=", Math.max(0, fill - 1)));
        builder.append('>');
        builder.append(StringUtil.repeat(" ", Math.max(0, 50 - fill - 1)));
        builder.append(String.format("] (%d%%)", percentage));

        return builder.toString().trim();
    }

    public ProgressBar(VarLightPlugin plugin, String name, long totalUnits) {
        this.plugin = plugin;
        this.id = ID_TICKER.getAndIncrement();
        this.name = name;
        this.totalUnits = totalUnits;
    }

    public void subscribeAll(@Nullable Collection<CommandSender> subscribers) {
        if (subscribers != null) {
            subscribers.forEach(this::subscribe);
        }
    }

    public void subscribe(CommandSender sender) {
        subscribers.add(sender);
    }

    public void advanceTo(long n) {
        this.completedUnits = n;

        checkUpdateNotifySubscribers();
    }

    public void step() {
        step(1);
    }

    public void step(long n) {
        this.completedUnits += n;

        checkUpdateNotifySubscribers();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgressBar)) return false;

        ProgressBar that = (ProgressBar) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public void close() {
        if (keepOnScreenTask != null) {
            keepOnScreenTask.cancel();
            keepOnScreenTask = null;
        }

        subscribers.clear();
    }

    private int getPercentage() {
        return (int) (100d * ((double) completedUnits) / ((double) totalUnits));
    }

    private void checkUpdateNotifySubscribers() {
        int perc = getPercentage();

        if (perc != lastPercent) {
            notifySubscribers(perc);
        }

        lastPercent = perc;
    }

    private void notifySubscribers(int percentage) {
        if (keepOnScreenTask != null) {
            keepOnScreenTask.cancel();
            keepOnScreenTask = null;
        }

        if (subscribers.isEmpty()) {
            return;
        }

        Runnable r = () -> {
            BaseComponent[] formatted = generateFormattedProgressBar(this.name, percentage);

            for (CommandSender subscriber : subscribers) {
                if (subscriber instanceof Player) {
                    ((Player) subscriber).spigot().sendMessage(ChatMessageType.ACTION_BAR, formatted);
                } else {
                    subscriber.spigot().sendMessage(formatted);
                }
            }
        };

        r.run(); // Run once
        keepOnScreenTask = Bukkit.getScheduler().runTaskTimer(plugin, r, Ticks.calculate(1, TimeUnit.SECONDS), Ticks.calculate(1, TimeUnit.SECONDS));
    }
}
