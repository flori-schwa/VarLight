package me.shawlaf.varlight.util.collections;

import java.util.Iterator;

public interface PredictableSizeIterator<E> extends Iterator<E> {

    int getSize();

}
