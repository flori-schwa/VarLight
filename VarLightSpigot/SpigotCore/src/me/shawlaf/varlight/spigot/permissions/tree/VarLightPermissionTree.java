package me.shawlaf.varlight.spigot.permissions.tree;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.spigot.permissions.PermissionNode;
import org.bukkit.permissions.PermissionDefault;

@UtilityClass
public class VarLightPermissionTree {

    // region Intermediary Permission Nodes

    private final PermissionNode ROOT = new PermissionNode(
            null,
            "varlight",
            "VarLight root Permission Node",
            PermissionDefault.FALSE
    );

    private final PermissionNode ADMIN_ROOT = new PermissionNode(
            ROOT,
            "admin",
            "Provides access to all VarLight Administrative Features",
            PermissionDefault.OP,
            false
    );

    private final PermissionNode PLAYER_ROOT = new PermissionNode(
            ROOT,
            "player",
            "Provides access to all non-administrative Features of VarLight",
            PermissionDefault.OP,
            false
    );

    // endregion

    public final PermissionNode CONFIG = new PermissionNode(
            ADMIN_ROOT,
            "config",
            "Provides access to all VarLight Configuration Options"
    );

    public final PermissionNode SAVE = new PermissionNode(
            ADMIN_ROOT,
            "save",
            "Provides access to all VarLight Save Options"
    );

    public final PermissionNode DEBUG = new PermissionNode(
            ADMIN_ROOT,
            "debug",
            "Provides access to all VarLight Debug Features"
    );

    public final PermissionNode MODIFY = new PermissionNode(
            ADMIN_ROOT,
            "modify",
            "Provides access to all VarLight Commands to modify Light sources"
    );

    public final PermissionNode MODIFY_SINGLE = new PermissionNode(
            MODIFY,
            "single",
            "Provides access to the /varlight update command"
    );

    public final PermissionNode GIVE = new PermissionNode(
            ADMIN_ROOT,
            "give",
            "Provides access to /varlight give"
    );

    public final PermissionNode STEP_SIZE = new PermissionNode(
            PLAYER_ROOT,
            "stepsize",
            "Provides access to /varlight stepsize"
    );

    public final PermissionNode USE = new PermissionNode(
            PLAYER_ROOT,
            "use",
            "Allows usage of the Light Update Item to modify Light Sources",
            PermissionDefault.TRUE
    );


    public void init() {
        ROOT.register();
    }

}
