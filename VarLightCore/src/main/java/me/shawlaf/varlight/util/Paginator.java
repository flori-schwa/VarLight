package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.util.collections.CountingIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@UtilityClass
public class Paginator {

    public static int getAmountPages(Collection<?> all, int pageSize) {
        return getAmountPages(all.size(), pageSize);
    }

    public static int getAmountPages(int total, int pageSize) {
        return Math.max(1, (total / pageSize) + (total % pageSize > 0 ? 1 : 0));
    }

    public static <T> Iterator<T> paginateEntriesIterator(CountingIterator<T> iterator, int pageSize, int page) {
        skipToPage(iterator, pageSize, page);
        List<T> tmp = new ArrayList<>(pageSize);

        for (int i = 0; i < pageSize && iterator.hasNext(); i++) {
            tmp.add(iterator.next());
        }

        return tmp.iterator();
    }

    public static <T> void skipToPage(CountingIterator<T> iterator, int pageSize, int page) {
        int startIndex = (page - 1) * (pageSize);

        if (iterator.getCount() > startIndex) {
            throw new IllegalStateException("Cannot traverse iterator backwards");
        }

        while (iterator.getCount() < startIndex) {
            if (!iterator.hasNext()) {
                throw new IndexOutOfBoundsException();
            }

            iterator.next();
        }
    }

    public static <T> List<T> paginateEntries(List<T> all, int pageSize, int page) {
        List<T> toReturn = new ArrayList<>();

        int start = (page - 1) * (pageSize);

        for (int i = 0, index = start; i < pageSize; i++, ++index) {
            if (index >= all.size()) {
                break;
            }

            toReturn.add(all.get(index));
        }

        return toReturn;
    }

}
