package me.shawlaf.varlight.spigot.permissions.tree;

import me.shawlaf.varlight.spigot.permissions.PermissionNode;
import org.bukkit.permissions.PermissionDefault;

public final class VarLightPermissionTree {

    // region Intermediary Permission Nodes

    private static final PermissionNode ROOT = new PermissionNode(
            null,
            "varlight",
            "VarLight root Permission Node",
            PermissionDefault.FALSE
    );

    private static final PermissionNode ADMIN_ROOT = new PermissionNode(
            ROOT,
            "admin",
            "Provides access to all VarLight Administrative Features",
            PermissionDefault.OP,
            false
    );

    private static final PermissionNode PLAYER_ROOT = new PermissionNode(
            ROOT,
            "player",
            "Provides access to all non-administrative Features of VarLight",
            PermissionDefault.OP,
            false
    );

    // endregion

    public static final PermissionNode CONFIG = new PermissionNode(
            ADMIN_ROOT,
            "config",
            "Provides access to all VarLight Configuration Options"
    );

    public static final PermissionNode SAVE = new PermissionNode(
            ADMIN_ROOT,
            "save",
            "Provides access to all VarLight Save Options"
    );

    public static final PermissionNode DEBUG = new PermissionNode(
            ADMIN_ROOT,
            "debug",
            "Provides access to all VarLight Debug Features"
    );

    public static final PermissionNode MODIFY = new PermissionNode(
            ADMIN_ROOT,
            "modify",
            "Provides access to all VarLight Commands to modify Light sources"
    );

    public static final PermissionNode MODIFY_SINGLE = new PermissionNode(
            MODIFY,
            "single",
            "Provides access to the /varlight update command"
    );

    public static final PermissionNode GIVE = new PermissionNode(
            ADMIN_ROOT,
            "give",
            "Provides access to /varlight give"
    );

    public static final PermissionNode STEP_SIZE = new PermissionNode(
            PLAYER_ROOT,
            "stepsize",
            "Provides access to /varlight stepsize"
    );

    public static final PermissionNode USE = new PermissionNode(
            PLAYER_ROOT,
            "use",
            "Allows usage of VarLight Gameplay features (LUI / Reclaim)",
            PermissionDefault.TRUE
    );

    public static final PermissionNode USE_ITEM = new PermissionNode(
            USE,
            "item",
            "Allows usage of the VarLight Light Update Item (LUI)",
            PermissionDefault.TRUE
    );

    public static final PermissionNode USE_RECLAIM = new PermissionNode(
            USE,
            "reclaim",
            "Allows players to retrieve Glowstone Dust / Glowing Variants of Blocks when breaking Custom Light sources",
            PermissionDefault.TRUE
    );


    public static void init() {
        ROOT.register();
    }

    private VarLightPermissionTree() {

    }

}
