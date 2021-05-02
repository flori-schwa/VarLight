package me.shawlaf.varlight.spigot.api;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@ExtensionMethod({
        Objects.class
})
public class LightUpdateResult {

    @NotNull
    @Getter
    private final LightUpdateResultType resultType;
    @Getter
    private final int fromLight, toLight;
    @Nullable
    private final VarLightNotActiveException exception;

    public static LightUpdateResult invalidBlock(int fromLight, int toLight) {
        return new LightUpdateResult(LightUpdateResultType.INVALID_BLOCK, fromLight, toLight, null);
    }

    public static LightUpdateResult cancelled(int fromLight, int toLight) {
        return new LightUpdateResult(LightUpdateResultType.CANCELLED, fromLight, toLight, null);
    }

    public static LightUpdateResult zeroReached(int fromLight, int toLight) {
        return new LightUpdateResult(LightUpdateResultType.ZERO_REACHED, fromLight, toLight, null);
    }

    public static LightUpdateResult fifteenReached(int fromLight, int toLight) {
        return new LightUpdateResult(LightUpdateResultType.FIFTEEN_REACHED, fromLight, toLight, null);
    }

    public static LightUpdateResult updated(int fromLight, int toLight) {
        return new LightUpdateResult(LightUpdateResultType.UPDATED, fromLight, toLight, null);
    }

    public static LightUpdateResult notActive(int fromLight, int toLight, @NotNull VarLightNotActiveException exception) {
        return new LightUpdateResult(LightUpdateResultType.NOT_ACTIVE, fromLight, toLight, exception.requireNonNull("Must provide a VarLightNotActiveException for NOT_ACTIVE Result"));
    }

    private LightUpdateResult(@NotNull LightUpdateResultType resultType, int fromLight, int toLight, @Nullable VarLightNotActiveException exception) {
        this.resultType = resultType.requireNonNull("resultType may not be null");
        this.fromLight = fromLight;
        this.toLight = toLight;
        this.exception = exception;
    }

    public boolean isSuccess() {
        return resultType.isSuccess();
    }

    @Nullable
    public String getMessage() { // TODO localize
        switch (this.resultType) {
            case INVALID_BLOCK:
            case CANCELLED:
                return null;
            case ZERO_REACHED:
                return "Cannot decrease Light level below 0.";
            case FIFTEEN_REACHED:
                return "Cannot increase Light level beyond 15.";
            case UPDATED:
                return String.format("Updated Light level to %d", toLight);
            case NOT_ACTIVE:
                return exception.requireNonNull().getMessage();
            default:
                throw new IllegalStateException(String.format("Reached default block (%s)", resultType.toString()));
        }
    }

    public void displayMessage(@NotNull CommandSender sender) {
        sender.requireNonNull();

        String message = getMessage();

        if (message == null) {
            return;
        }

        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } else {
            sender.sendMessage(message);
        }
    }
}
