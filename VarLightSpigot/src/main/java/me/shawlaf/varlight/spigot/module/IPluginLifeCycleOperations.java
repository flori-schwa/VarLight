package me.shawlaf.varlight.spigot.module;

public interface IPluginLifeCycleOperations {

    default void onLoad() { }

    default void onEnable() { }

    default void onDisable() { }

}
