package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;

public class SketchDetailProjection extends SketchDetail {

    public SketchDetailProjection() {
        super(Colour.NONE);
    }

    public static SketchDetailProjection create(Space<Coord2D> projection) {
        SketchDetailProjection detail = new SketchDetailProjection();
        for (Leg leg: projection.getLegMap().keySet()) {
            Line<Coord2D> line = projection.getLegMap().get(leg);
            detail.updateBoundingBox(line.getStart());
            detail.updateBoundingBox(line.getEnd());
        }

        return detail;
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        return 0;
    }

    @Override
    public SketchDetail translate(Coord2D point) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SketchDetail scale(float scale) { throw new UnsupportedOperationException(); }
}
