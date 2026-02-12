package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;

public class LayerSwitchDetail extends SketchDetail {

    private final int fromLayerId;
    private final int toLayerId;

    public LayerSwitchDetail(int fromLayerId, int toLayerId) {
        super(Colour.NONE);
        this.fromLayerId = fromLayerId;
        this.toLayerId = toLayerId;
    }

    public int getFromLayerId() {
        return fromLayerId;
    }

    public int getToLayerId() {
        return toLayerId;
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        return Float.MAX_VALUE;
    }

    @Override
    public SketchDetail translate(Coord2D point) {
        return this;
    }

    @Override
    public SketchDetail scale(float scale) {
        return this;
    }
}
