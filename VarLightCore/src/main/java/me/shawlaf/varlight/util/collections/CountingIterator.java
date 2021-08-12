package me.shawlaf.varlight.util.collections;

import lombok.Getter;

import java.util.Iterator;

public class CountingIterator<T> implements Iterator<T> {

    @Getter
    private int count;
    private Iterator<T> base;

    public CountingIterator(Iterator<T> base) {
        this.base = base;
    }

    public int countToEnd() {
        while (hasNext()) {
            next();
        }

        return count;
    }

    @Override
    public boolean hasNext() {
        return base.hasNext();
    }

    @Override
    public T next() {
        T result = base.next();
        ++count;
        return result;
    }

    @Override
    public void remove() {
        base.remove();
        --count;
    }
}
