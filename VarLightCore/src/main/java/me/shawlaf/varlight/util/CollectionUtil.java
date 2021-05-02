package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CollectionUtil {
    public static <E> List<E> toList(E[] array) {
        List<E> list = new ArrayList<>(array.length);

        for (int i = 0; i < array.length; i++) {
            list.add(i, array[i]);
        }

        return list;
    }
}
