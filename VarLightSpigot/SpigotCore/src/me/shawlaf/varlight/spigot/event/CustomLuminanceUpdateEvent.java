package me.shawlaf.varlight.spigot.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

public class CustomLuminanceUpdateEvent extends BlockEvent implements Cancellable {

    public static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    @Getter
    private final int fromLight;
    @Getter @Setter
    private int toLight;

    public CustomLuminanceUpdateEvent(@NotNull Block theBlock, int fromLight, int toLight) {
        super(theBlock);
        this.fromLight = fromLight;
        this.toLight = toLight;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
