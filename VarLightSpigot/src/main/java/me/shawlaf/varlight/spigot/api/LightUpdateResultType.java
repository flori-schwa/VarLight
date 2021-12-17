package me.shawlaf.varlight.spigot.api;

import lombok.Getter;
import me.shawlaf.varlight.spigot.util.NamespacedID;

public enum LightUpdateResultType {

    INVALID_BLOCK(NamespacedID.varlight("invalid_block")),
    CANCELLED(NamespacedID.varlight("cancelled")),
    ZERO_REACHED(NamespacedID.varlight("zero_reached")),
    FIFTEEN_REACHED(NamespacedID.varlight("fifteen_reached")),
    NOT_ACTIVE(NamespacedID.varlight("not_active")),
    UPDATED(NamespacedID.varlight("updated"));

    @Getter
    private final NamespacedID id;

    LightUpdateResultType(NamespacedID id) {
        this.id = id;
    }

    public boolean isSuccess() {
        return this == UPDATED;
    }

}
