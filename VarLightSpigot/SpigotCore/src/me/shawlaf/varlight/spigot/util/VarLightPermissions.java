package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.spigot.permissions.VarLightPermissionTree;
import org.bukkit.permissions.Permissible;

@UtilityClass
public class VarLightPermissions {

    public boolean hasVarLightUsePermission(Permissible self) {
        return VarLightPermissionTree.instance().player.use.hasPermission(self);
    }

    public boolean hasVarLightDebugPermission(Permissible self) {
        return VarLightPermissionTree.instance().admin.debug.debugBase.hasPermission(self);
    }

}
