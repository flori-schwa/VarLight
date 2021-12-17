package me.shawlaf.varlight.spigot.permissions;

import lombok.Getter;
import org.bukkit.permissions.Permissible;

public class NoPermissionException extends Exception {

    @Getter
    private final Permissible permissible;
    @Getter
    private final PermissionNode node;

    public NoPermissionException(Permissible permissible, PermissionNode node) {
        super(String.format("%s does not have %s permission", permissible.toString(), node.getFullName()));

        this.permissible = permissible;
        this.node = node;
    }
}
