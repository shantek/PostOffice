package io.shantek.functions;

import java.util.UUID;

public class BarrelData {
    private final UUID ownerUUID;
    private final String signLocation;
    private final String state;

    public BarrelData(UUID ownerUUID, String state, String signLocation) {
        this.ownerUUID = ownerUUID;
        this.signLocation = signLocation;
        this.state = state;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getSignLocation() {
        return signLocation;
    }

    public String getState() {
        return state;
    }
}
