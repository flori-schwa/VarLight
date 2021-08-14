package me.shawlaf.varlight.spigot.bulk;

import lombok.Getter;
import me.shawlaf.command.result.CommandResult;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BulkFillTaskResult {

    @NotNull
    @Getter
    private final BulkFillTask task;

    @NotNull
    @Getter
    private final Type resultType;
    @Getter
    @NotNull
    private final CommandResult result;

    public BulkFillTaskResult(@NotNull BulkFillTask task, @NotNull Type resultType, @NotNull CommandResult result) {
        this.task = task;
        this.resultType = resultType;
        this.result = result;
    }

    public boolean isSucess() {
        return resultType.isSuccess();
    }

    public void finish(CommandSender sender) {
        result.finish(sender);
    }

    public enum Type {
        NOT_ACTIVE,
        TOO_LARGE,
        SUCCESS,
        ERROR;

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
}
