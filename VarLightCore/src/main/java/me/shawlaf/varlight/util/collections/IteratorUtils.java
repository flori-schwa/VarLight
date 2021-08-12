package me.shawlaf.varlight.util.collections;

import lombok.experimental.UtilityClass;

import java.util.Iterator;
import java.util.stream.Collector;

@UtilityClass
public class IteratorUtils {

    public <T, A, R> R collectFromIterator(Iterator<T> iterator, Collector<T, A, R> collector) {
        A resultContainer = collector.supplier().get();

        while (iterator.hasNext()) {
            T next = iterator.next();

            collector.accumulator().accept(resultContainer, next);
        }

        return collector.finisher().apply(resultContainer);
    }

}
