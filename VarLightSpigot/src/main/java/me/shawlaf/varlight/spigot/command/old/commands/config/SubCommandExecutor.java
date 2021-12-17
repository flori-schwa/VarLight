package me.shawlaf.varlight.spigot.command.old.commands.config;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;

public class SubCommandExecutor {

    protected final VarLightSubCommand command;
    protected final VarLightPlugin plugin;

    public SubCommandExecutor(VarLightSubCommand command) {
        this.command = command;
        this.plugin = command.getPlugin();
    }

}
