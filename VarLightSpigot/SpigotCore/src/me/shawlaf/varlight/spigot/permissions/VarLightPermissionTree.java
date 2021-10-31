package me.shawlaf.varlight.spigot.permissions;

import org.bukkit.permissions.PermissionDefault;

public class VarLightPermissionTree {

    private static VarLightPermissionTree INSTANCE;

    public static VarLightPermissionTree instance() {
        if (INSTANCE == null) {
            INSTANCE = new VarLightPermissionTree();
        }

        return INSTANCE;
    }

    public final PermissionNode base = new PermissionNode(
            null,
            "varlight",
            "VarLight root Permission Node",
            PermissionDefault.FALSE
    );

    public final Admin admin = this.new Admin();
    public final Player player = this.new Player();

    public class Admin {
        public final PermissionNode adminBase = new PermissionNode(
                base,
                "admin",
                "Provides access to all VarLight Administrative Features",
                PermissionDefault.OP,
                false
        );

        public final Config config = this.new Config();
        public final Save save = this.new Save();
        public final Debug debug = this.new Debug();
        public final Modify modify = this.new Modify();
        public final Give give = this.new Give();

        public class Config {
            public final PermissionNode configBase = new PermissionNode(
                    adminBase,
                    "config",
                    "Provides access to all VarLight Configuration Options"
            );
        }

        public class Save {
            public final PermissionNode saveBase = new PermissionNode(
                    adminBase,
                    "save",
                    "Provides access to all VarLight Save Options"
            );
        }

        public class Debug {
            public final PermissionNode debugBase = new PermissionNode(
                    adminBase,
                    "debug",
                    "Provides access to all VarLight Debug Features",
                    PermissionDefault.OP
            );
        }

        public class Modify {
            public final PermissionNode modifyBase = new PermissionNode(
                    adminBase,
                    "modify",
                    "Provides access to all VarLight Commands to modify Light sources"
            );

            public final PermissionNode single = new PermissionNode(
                    modifyBase,
                    "single",
                    "Provides access to the /varlight update command"
            );
        }
        public class Give {
            public final PermissionNode giveBase = new PermissionNode(
                    adminBase,
                    "give",
                    "Provides access to /varlight give"
            );
        }
    }

    public class Player {
        public final PermissionNode playerBase = new PermissionNode(
                base,
                "player",
                "Provides access to all non-administrative Features of VarLight",
                PermissionDefault.OP,
                false
        );

        public final PermissionNode stepsize = new PermissionNode(
                playerBase,
                "stepsize",
                "Provides access to /varlight stepsize"
        );

        public final PermissionNode use = new PermissionNode(
                playerBase,
                "use",
                "Allows usage of the Light Update Item to modify Light Sources",
                PermissionDefault.TRUE
        );
    }


}
