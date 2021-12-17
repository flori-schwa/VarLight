package me.shawlaf.varlight.spigot.util;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class NamespacedID {

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[0-9a-z_\\-\\.]+");
    private static final Pattern NAME_PATTERN = Pattern.compile("[0-9a-z_/\\-\\.]+");

    @NotNull @Getter
    private final String namespace;
    @NotNull @Getter
    private final String name;

    private static boolean isLegalNamespace(String namespace) {
        return NAMESPACE_PATTERN.matcher(namespace).matches();
    }

    private static boolean isLegalName(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

    public static NamespacedID varlight(String name) {
        return new NamespacedID("varlight", name);
    }

    public static NamespacedID fromBukkit(NamespacedKey key) {
        return new NamespacedID(key.getNamespace(), key.getKey());
    }

    public NamespacedID(@NotNull String fullKey) {
        requireNonNull(fullKey, "fullKey may not be null");

        if (fullKey.contains(":")) {
            String[] parts = fullKey.split(":", 2);

            String namespace = parts[0];
            String name = parts[1];

            if (!isLegalNamespace(namespace)) {
                throw new IllegalArgumentException(String.format("%s is an illegal Namespace", namespace));
            }

            if (!isLegalName(name)) {
                throw new IllegalArgumentException(String.format("%s is an illegal Name", name));
            }

            this.namespace = namespace;
            this.name = name;
        } else {
            if (!isLegalName(fullKey)) {
                throw new IllegalArgumentException(String.format("%s is an illegal Name", fullKey));
            }

            this.namespace = "minecraft";
            this.name = fullKey;
        }
    }

    public NamespacedID(@NotNull String namespace, @NotNull String name) {
        this.namespace = requireNonNull(namespace, "Namespace may not be null");
        this.name = requireNonNull(name, "Name may not be null");

        if (!isLegalNamespace(namespace)) {
            throw new IllegalArgumentException(String.format("%s is an illegal Namespace", namespace));
        }

        if (!isLegalName(name)) {
            throw new IllegalArgumentException(String.format("%s is an illegal Name", name));
        }
    }

    @SuppressWarnings("Deprecation")
    public NamespacedKey toBukkitKey() {
        return new NamespacedKey(this.namespace, this.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamespacedID that = (NamespacedID) o;
        return namespace.equals(that.namespace) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", namespace, name);
    }
}
