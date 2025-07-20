package me.shawlaf.varlight.spigot.permissions;

import org.bukkit.permissions.Permissible;

public class NoPermissionException extends Exception {

    private final Permissible permissible;
    private final PermissionNode node;

    public NoPermissionException(Permissible permissible, PermissionNode node) {
        super(String.format("%s does not have %s permission", permissible.toString(), node.getFullName()));

        this.permissible = permissible;
        this.node = node;
    }

    public Permissible getPermissible() {
        return permissible;
    }

    public PermissionNode getNode() {
        return node;
    }
}
