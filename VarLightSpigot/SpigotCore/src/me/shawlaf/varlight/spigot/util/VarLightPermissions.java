package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import org.bukkit.permissions.Permissible;

@UtilityClass
public class VarLightPermissions {

    public boolean hasVarLightUsePermission(Permissible self) {
        return self.hasPermission("varlight.use");
    }

}
