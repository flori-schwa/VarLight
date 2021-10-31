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

    public final PermissioneNode base = new PermissioneNode(
            null,
            "varlight",
            "VarLight root Permission Node",
            PermissionDefault.FALSE
    );

    public final Admin admin = this.new Admin();
    public final Player player = this.new Player();

    public class Admin {
        public final PermissioneNode adminBase = new PermissioneNode(
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
            public final PermissioneNode configBase = new PermissioneNode(
                    adminBase,
                    "config",
                    "Provides access to all VarLight Configuration Options"
            );
        }

        public class Save {
            public final PermissioneNode saveBase = new PermissioneNode(
                    adminBase,
                    "save",
                    "Provides access to all VarLight Save Options"
            );
        }

        public class Debug {
            public final PermissioneNode debugBase = new PermissioneNode(
                    adminBase,
                    "debug",
                    "Provides access to all VarLight Debug Features",
                    PermissionDefault.OP
            );
        }

        public class Modify {
            public final PermissioneNode modifyBase = new PermissioneNode(
                    adminBase,
                    "modify",
                    "Provides access to all VarLight Commands to modify Light sources"
            );

            public final PermissioneNode single = new PermissioneNode(
                    modifyBase,
                    "single",
                    "Provides access to the /varlight update command"
            );
        }
        public class Give {
            public final PermissioneNode giveBase = new PermissioneNode(
                    adminBase,
                    "give",
                    "Provides access to /varlight give"
            );
        }
    }

    public class Player {
        public final PermissioneNode playerBase = new PermissioneNode(
                base,
                "player",
                "Provides access to all non-administrative Features of VarLight",
                PermissionDefault.OP,
                false
        );

        public final PermissioneNode stepsize = new PermissioneNode(
                playerBase,
                "stepsize",
                "Provides access to /varlight stepsize"
        );

        public final PermissioneNode use = new PermissioneNode(
                playerBase,
                "use",
                "Allows usage of the Light Update Item to modify Light Sources",
                PermissionDefault.TRUE
        );
    }


}
