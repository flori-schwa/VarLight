package me.shawlaf.varlight.spigot.event;

import org.bukkit.command.CommandSender;

@Deprecated // Replace with sealed interface
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

    private final CommandSender playerCause, commandCause;
    private final Type cause;
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

    public CommandSender getPlayerCause() {
        return playerCause;
    }

    public CommandSender getCommandCause() {
        return commandCause;
    }

    public Type getCause() {
        return cause;
    }

    public PlayerAction getPlayerAction() {
        return playerAction;
    }
}
