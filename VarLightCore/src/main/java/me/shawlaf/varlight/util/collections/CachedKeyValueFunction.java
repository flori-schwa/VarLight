package me.shawlaf.varlight.util.collections;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class CachedKeyValueFunction<K, V> {

    private final Map<K, V> wrapped;

    public CachedKeyValueFunction(Map<K, V> wrapped) {
        this.wrapped = wrapped;
    }

    public CachedKeyValueFunction() {
        this.wrapped = new HashMap<>();
    }

    @NotNull
    public V lookup(K key) {
        V val = wrapped.get(key);

        if (val == null) {
            val = Objects.requireNonNull(constructNew(key));
            wrapped.put(key, val);
        }

        return val;
    }

    public boolean contains(K key) {
        return wrapped.containsKey(key);
    }

    public boolean invalidate(K key) {
        return this.wrapped.remove(key) != null;
    }

    @NotNull
    protected abstract V constructNew(K key);


}
