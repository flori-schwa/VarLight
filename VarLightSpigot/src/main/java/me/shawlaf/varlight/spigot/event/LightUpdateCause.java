package me.shawlaf.varlight.spigot.event;

import lombok.Getter;
import org.bukkit.command.CommandSender;

public class LightUpdateCause {

    public enum Type {
        PLAYER,
        COMMAND,
        API
    }

    @Getter
    private final CommandSender playerCause, commandCause;
    @Getter
    private final Type cause;

    public static LightUpdateCause player(CommandSender player) {
        return new LightUpdateCause(player, null, Type.PLAYER);
    }

    public static LightUpdateCause command(CommandSender source) {
        return new LightUpdateCause(null, source, Type.COMMAND);
    }

    public static LightUpdateCause api() {
        return new LightUpdateCause(null, null, Type.API);
    }

    private LightUpdateCause(CommandSender playerCause, CommandSender commandCause, Type cause) {
        this.playerCause = playerCause;
        this.commandCause = commandCause;
        this.cause = cause;
    }
}
