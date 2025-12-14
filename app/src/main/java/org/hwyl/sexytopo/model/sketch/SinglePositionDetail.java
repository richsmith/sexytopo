package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;


public abstract class SinglePositionDetail extends SketchDetail {

    protected final Coord2D position;

    protected SinglePositionDetail(Colour colour, Coord2D position) {
        super(colour);
        this.position = position;
        updateBoundingBox(position);
    }

    public Coord2D getPosition() {
        return position;
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        return Space2DUtils.getDistance(point, position);
    }

    @Override
    public SinglePositionDetail scale(float scale) {
        // if just a point, do we need to scale anything?
        // override in subclass if necessary
        return this;
    }
}
