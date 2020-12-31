package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtil {
    public static int modulo(int a, int b) {
        return ((a % b) + b) % b;
    }
}
