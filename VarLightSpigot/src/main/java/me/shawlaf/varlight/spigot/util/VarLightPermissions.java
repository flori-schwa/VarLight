package me.shawlaf.varlight.spigot.util;

import me.shawlaf.varlight.spigot.api.IVarLightAPI;
import me.shawlaf.varlight.spigot.permissions.tree.VarLightPermissionTree;
import org.bukkit.permissions.Permissible;

public final class VarLightPermissions {

    private VarLightPermissions() {

    }

    public static boolean mayUseLui(Permissible self) {
        if (!IVarLightAPI.getAPI().getConfiguration().isCheckingPermission()) {
            return true;
        }

        return VarLightPermissionTree.USE_ITEM.hasPermission(self);
    }

    public static boolean mayReclaimLui(Permissible self) {
        if (!IVarLightAPI.getAPI().getConfiguration().isCheckingPermission()) {
            return true;
        }

        return VarLightPermissionTree.USE_RECLAIM.hasPermission(self);
    }

    public static boolean hasVarLightDebugPermission(Permissible self) {
        return VarLightPermissionTree.DEBUG.hasPermission(self);
    }

}
