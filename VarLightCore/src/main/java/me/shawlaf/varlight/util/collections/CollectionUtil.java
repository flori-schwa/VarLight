package me.shawlaf.varlight.util.collections;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@UtilityClass
public class CollectionUtil {
    public <E> List<E> toList(E[] array) {
        List<E> list = new ArrayList<>(array.length);

        for (int i = 0; i < array.length; i++) {
            list.add(i, array[i]);
        }

        return list;
    }

    public <T> CountingIterator<T> count(Iterator<T> iterator) {
        return new CountingIterator<>(iterator);
    }
}
