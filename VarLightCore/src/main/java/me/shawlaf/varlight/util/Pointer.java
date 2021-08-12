package me.shawlaf.varlight.util;

public class Pointer<T> {

    public T value;

    public Pointer(T t) {
        this.value = t;
    }

    public Pointer() {
        this(null);
    }
}
