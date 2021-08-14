package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import org.bukkit.permissions.Permissible;

@UtilityClass
public class VarLightPermissions {

    public static final String VARLIGHT_USE_PERMISSION = "varlight.use";
    public static final String VARLIGHT_DEBUG_PERMISSION = "varlight.admin.debug";
    public static final String VARLIGHT_FILL_PERMISSION = "varlight.admin.fill";

    public boolean hasVarLightUsePermission(Permissible self) {
        return self.hasPermission(VARLIGHT_USE_PERMISSION);
    }

    public boolean hasVarLightDebugPermission(Permissible self) {
        return self.hasPermission(VARLIGHT_DEBUG_PERMISSION);
    }

}
