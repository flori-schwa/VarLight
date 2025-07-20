package me.shawlaf.varlight.spigot.bulk;

import me.shawlaf.command.result.CommandResult;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// TODO Convert to sealed interface
public class BulkTaskResult {

    @NotNull
    private final AbstractBulkTask task;

    @NotNull
    private final Type resultType;
    @NotNull
    private final CommandResult result;

    public BulkTaskResult(@NotNull AbstractBulkTask task, @NotNull Type resultType, @NotNull CommandResult result) {
        this.task = Objects.requireNonNull(task);
        this.resultType = Objects.requireNonNull(resultType);
        this.result = Objects.requireNonNull(result);
    }

    public @NotNull AbstractBulkTask getTask() {
        return task;
    }

    public @NotNull Type getResultType() {
        return resultType;
    }

    public @NotNull CommandResult getResult() {
        return result;
    }

    public boolean isSuccess() {
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
