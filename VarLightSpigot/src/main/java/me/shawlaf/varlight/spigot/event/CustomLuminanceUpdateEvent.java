package me.shawlaf.varlight.spigot.event;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

public class CustomLuminanceUpdateEvent extends BlockEvent implements Cancellable {

    public static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    private final int fromLight;
    private int toLight;
    private LightUpdateCause cause;

    public CustomLuminanceUpdateEvent(@NotNull Block theBlock, int fromLight, int toLight, LightUpdateCause cause) {
        super(theBlock);
        this.fromLight = fromLight;
        this.toLight = toLight;
        this.cause = cause;
    }

    public int getFromLight() {
        return fromLight;
    }

    public int getToLight() {
        return toLight;
    }

    public void setToLight(int toLight) {
        this.toLight = toLight;
    }

    public LightUpdateCause getCause() {
        return cause;
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
