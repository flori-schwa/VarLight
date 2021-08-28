package me.shawlaf.varlight.spigot.util.collections;

import me.shawlaf.varlight.util.collections.DelegatedKeyHashMap;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class EntityToObjectMap<V> extends DelegatedKeyHashMap<Entity, UUID, V> {

    public EntityToObjectMap() {
        super(Entity.class);
    }

    @Override
    protected UUID getKey(Entity key) {
        return key.getUniqueId();
    }
}
