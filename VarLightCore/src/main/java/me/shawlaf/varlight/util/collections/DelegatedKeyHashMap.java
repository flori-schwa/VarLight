package me.shawlaf.varlight.util.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class DelegatedKeyHashMap<K, D, V> implements Map<K, V> {

    private Map<D, V> wrapped = new HashMap<>();

    private final Class<K> keyClass;

    public DelegatedKeyHashMap(Class<K> keyClass) {
        this.keyClass = keyClass;
    }

    protected abstract D getKey(K key);

    protected K reverseKeyFunc(D delegatedKey) {
        throw new UnsupportedOperationException("Cannot convert from delegated key to original key");
    }

    @SuppressWarnings("unchecked")
    private Object getKey0(Object key) {
        if (keyClass.isAssignableFrom(key.getClass())) {
            return getKey(((K) key));
        }

        return key;
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return wrapped.containsKey(getKey0(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return wrapped.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return wrapped.get(getKey0(key));
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        return putDelegatedKey(getKey(key), value);
    }

    @Nullable
    public V putDelegatedKey(D key, V value) {
        return wrapped.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return wrapped.remove(getKey0(key));
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        wrapped.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return wrapped.keySet().stream().map(this::reverseKeyFunc).collect(Collectors.toSet());
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return wrapped.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = new HashSet<>();

        for (Entry<D, V> dvEntry : wrapped.entrySet()) {
            entries.add(new AbstractMap.SimpleEntry<>(reverseKeyFunc(dvEntry.getKey()), dvEntry.getValue()));
        }

        return entries;
    }
}
