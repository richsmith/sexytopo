package org.hwyl.sexytopo.model.sketch;

/**
 * The kind of feature an AreaDetail represents. Currently only water, but structured as an enum so
 * further area types (sand, clay etc.) can be added later.
 */
public enum AreaType {
    WATER("water", true);

    private final String therionName;
    private final boolean isWater;

    AreaType(String therionName, boolean isWater) {
        this.therionName = therionName;
        this.isWater = isWater;
    }

    public String getTherionName() {
        return therionName;
    }

    public boolean isWater() {
        return isWater;
    }
}
