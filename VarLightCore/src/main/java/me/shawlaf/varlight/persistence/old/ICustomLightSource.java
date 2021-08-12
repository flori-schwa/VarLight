package me.shawlaf.varlight.persistence.old;

import me.shawlaf.varlight.util.pos.IntPosition;

@Deprecated
public interface ICustomLightSource {

    IntPosition getPosition();

    String getType();

    boolean isMigrated();

    int getCustomLuminance();

}
