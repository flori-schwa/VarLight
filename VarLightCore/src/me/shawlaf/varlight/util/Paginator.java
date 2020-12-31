package me.shawlaf.varlight.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@UtilityClass
public class Paginator {

    public static int getAmountPages(Collection<?> all, int pageSize) {
        return getAmountPages(all.size(), pageSize);
    }

    public static int getAmountPages(int total, int pageSize) {
        return Math.max(1, (total / pageSize) + (total % pageSize > 0 ? 1 : 0));
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
