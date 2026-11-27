package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.geometry.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;

public abstract class PointDetail extends SketchDetail {

    protected final Coord2D position;

    protected PointDetail(Colour colour, Coord2D position) {
        super(colour);
        this.position = position;
        updateBoundingBox(position);
    }

    public Coord2D getPosition() {
        return position;
    }

    @Override
    public GeometryType getGeometryType() {
        return GeometryType.POINT;
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        return Space2DUtils.getDistance(point, position);
    }

    @Override
    public PointDetail scale(float scale) {
        // if just a point, do we need to scale anything?
        // override in subclass if necessary
        return this;
    }
}
