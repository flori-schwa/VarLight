package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Preconditions {

    public static void assertInRange(String paramName, int value, int minIncl, int maxIncl) {
        if (value < minIncl) {
            throw new IllegalArgumentException(String.format("Parameter %s out range: must be >= %d", paramName, minIncl));
        }

        if (value > maxIncl) {
            throw new IllegalArgumentException(String.format("Parameter %s out of range: must be <= %d", paramName, maxIncl));
        }
    }

}
