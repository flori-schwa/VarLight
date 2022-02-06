package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.api.IVarLightAPI;
import me.shawlaf.varlight.spigot.permissions.tree.VarLightPermissionTree;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

@UtilityClass
public class VarLightPermissions {

    public boolean mayUseLui(Permissible self) {
        if (!IVarLightAPI.getAPI().getConfiguration().isCheckingPermission()) {
            return true;
        }

        return VarLightPermissionTree.USE_ITEM.hasPermission(self);
    }

    public boolean mayReclaimLui(Permissible self) {
        if (!IVarLightAPI.getAPI().getConfiguration().isCheckingPermission()) {
            return true;
        }

        return VarLightPermissionTree.USE_RECLAIM.hasPermission(self);
    }

    public boolean hasVarLightDebugPermission(Permissible self) {
        return VarLightPermissionTree.DEBUG.hasPermission(self);
    }

}
