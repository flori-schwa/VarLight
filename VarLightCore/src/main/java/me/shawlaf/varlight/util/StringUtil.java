package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@UtilityClass
public class StringUtil {

    public @NotNull String repeat(@NotNull String x, int times) {
        if (times < 0) {
            throw new IllegalArgumentException("times must be >= 0, got: " + times);
        }

        Objects.requireNonNull(x, "String must not be null");

        if (times == 0) {
            return "";
        }

        if (times == 1) {
            return x;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < times; i++) {
            builder.append(x);
        }

        return builder.toString();
    }

}
