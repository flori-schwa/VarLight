package me.shawlaf.varlight.spigot.permissions;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PermissionNode {

    @Nullable
    private final PermissionNode parent;
    @NotNull
    private final String name, fullName;

    @Nullable
    private String description;

    @NotNull
    private PermissionDefault permissionDefault = PermissionDefault.OP;
    private boolean inherited = true;

    private final HashMap<String, PermissionNode> children = new HashMap<>();

    public PermissionNode(@Nullable PermissionNode parent, @NotNull String name) {
        this.parent = parent;
        this.name = Objects.requireNonNull(name);

        if (parent != null) {
            this.fullName = String.format("%s.%s", parent.getFullName(), name);
            parent.children.put(name, this);
        } else {
            this.fullName = name;
        }
    }

    public PermissionNode(@Nullable PermissionNode parent, @NotNull String name, @Nullable String description, @NotNull PermissionDefault permissionDefault, boolean inherited) {
        this(parent, name);

        this.description = description;
        this.permissionDefault = permissionDefault;
        this.inherited = inherited;
    }

    public PermissionNode(@Nullable PermissionNode parent, @NotNull String name, @Nullable String description, @NotNull PermissionDefault permissionDefault) {
        this(parent, name, description, permissionDefault, true);
    }

    public PermissionNode(@Nullable PermissionNode parent, @NotNull String name, @Nullable String description) {
        this(parent, name, description, PermissionDefault.OP);
    }

    @Nullable
    public PermissionNode getParent() {
        return parent;
    }

    public @NotNull String getFullName() {
        return fullName;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public @NotNull PermissionDefault getPermissionDefault() {
        return permissionDefault;
    }

    public void setPermissionDefault(@NotNull PermissionDefault permissionDefault) {
        this.permissionDefault = permissionDefault;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public void register() {
        Map<String, Boolean> bukkitChildren = new HashMap<>();

        for (PermissionNode child : this.children.values()) {
            bukkitChildren.put(child.getFullName(), child.inherited);

            child.register();
        }

        Bukkit.getPluginManager().addPermission(new Permission(
                this.fullName,
                this.description,
                this.permissionDefault,
                bukkitChildren
        ));
    }

    public void assertHasPermission(Permissible permissible) throws NoPermissionException {
        if (!hasPermission(permissible)) {
            throw new NoPermissionException(permissible, this);
        }
    }

    public boolean hasPermission(Permissible permissible) {
        return permissible.hasPermission(this.fullName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionNode that = (PermissionNode) o;
        return fullName.equals(that.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName);
    }
}
