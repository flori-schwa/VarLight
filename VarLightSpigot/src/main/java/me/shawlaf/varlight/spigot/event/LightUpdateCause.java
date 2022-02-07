package me.shawlaf.varlight.spigot.event;

import lombok.Getter;
import org.bukkit.command.CommandSender;

public class LightUpdateCause {

    public enum Type {
        PLAYER,
        COMMAND,
        API
    }

    public enum PlayerAction {
        LUI,
        PLACE_LIGHT_SOURCE,
        UNSPECIFIED
    }

    @Getter
    private final CommandSender playerCause, commandCause;
    @Getter
    private final Type cause;
    @Getter
    private final PlayerAction playerAction;

    public static LightUpdateCause player(CommandSender player, PlayerAction playerAction) {
        return new LightUpdateCause(player, null, Type.PLAYER, playerAction);
    }

    public static LightUpdateCause command(CommandSender source) {
        return new LightUpdateCause(null, source, Type.COMMAND, null);
    }

    public static LightUpdateCause api() {
        return new LightUpdateCause(null, null, Type.API, null);
    }

    private LightUpdateCause(CommandSender playerCause, CommandSender commandCause, Type cause, PlayerAction playerAction) {
        this.playerCause = playerCause;
        this.commandCause = commandCause;
        this.cause = cause;
        this.playerAction = playerAction;
    }
}
