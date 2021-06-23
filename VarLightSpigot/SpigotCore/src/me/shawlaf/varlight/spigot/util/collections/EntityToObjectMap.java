package me.shawlaf.varlight.spigot.util.collections;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityToObjectMap<V> {

    private final Map<UUID, V> wrapped = new HashMap<>();

    public int size() {
        return wrapped.size();
    }

    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    public boolean containsKey(Entity key) {
        return wrapped.containsKey(key.getUniqueId());
    }

    public boolean containsValue(Object value) {
        return wrapped.containsValue(value);
    }

    public V get(Entity key) {
        return wrapped.get(key.getUniqueId());
    }

    @Nullable
    public V put(Entity key, V value) {
        return wrapped.put(key.getUniqueId(), value);
    }

    public V remove(Entity key) {
        return removeId(key.getUniqueId());
    }

    public V removeId(UUID id) {
        return wrapped.remove(id);
    }

    public void putAll(@NotNull Map<? extends Entity, ? extends V> m) {
        for (Map.Entry<? extends Entity, ? extends V> entry : m.entrySet()) {
            this.wrapped.put(entry.getKey().getUniqueId(), entry.getValue());
        }
    }

    public void clear() {
        this.wrapped.clear();
    }

    @NotNull
    public Set<@NotNull UUID> keySet() {
        return this.wrapped.keySet();
    }

    @NotNull
    public Collection<V> values() {
        return this.wrapped.values();
    }

    // TODO implement entrySet()

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityToObjectMap<?> that = (EntityToObjectMap<?>) o;
        return Objects.equals(wrapped, that.wrapped);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrapped);
    }
}
