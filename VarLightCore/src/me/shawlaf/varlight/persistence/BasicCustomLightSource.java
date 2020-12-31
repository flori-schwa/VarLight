package me.shawlaf.varlight.persistence;

import lombok.Getter;
import me.shawlaf.varlight.util.IntPosition;

import java.util.Objects;

@Deprecated
public class BasicCustomLightSource implements ICustomLightSource {
    @Getter
    private final IntPosition position;
    @Getter
    private final String type;
    private final int emittingLight;
    @Getter
    private final boolean migrated;

    public BasicCustomLightSource(IntPosition position, int emittingLight, boolean migrated, String type) {
        this.position = position;
        this.type = type;
        this.emittingLight = emittingLight;
        this.migrated = migrated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicCustomLightSource that = (BasicCustomLightSource) o;
        return emittingLight == that.emittingLight &&
                migrated == that.migrated &&
                position.equals(that.position) &&
                type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, type, emittingLight, migrated);
    }

    @Override
    public int getCustomLuminance() {
        return emittingLight;
    }

    @Override
    public String toString() {
        return "BasicStoredLightSource{" +
                "position=" + position +
                ", type='" + type + '\'' +
                ", emittingLight=" + emittingLight +
                ", migrated=" + migrated +
                '}';
    }
}