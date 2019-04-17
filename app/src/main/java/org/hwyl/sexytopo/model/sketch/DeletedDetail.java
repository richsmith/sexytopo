package org.hwyl.sexytopo.model.sketch;

import org.apache.commons.lang3.NotImplementedException;
import org.hwyl.sexytopo.model.graph.Coord2D;

import java.security.InvalidParameterException;


public class DeletedDetail extends SketchDetail {

    private SketchDetail sketchDetail;

    public DeletedDetail(SketchDetail sketchDetail) {
        super(Colour.BLACK);

        if (sketchDetail instanceof DeletedDetail) {
            throw new InvalidParameterException("Can't wrap a DeletedDetail in a DeletedDetail");
        }

        this.sketchDetail = sketchDetail;

    }

    @Override
    public double getDistanceFrom(Coord2D point) {
        return sketchDetail.getDistanceFrom(point);
    }


    @Override
    public SketchDetail translate(Coord2D point) {
        return new DeletedDetail(sketchDetail.translate(point));
    }


    public SketchDetail getSketchDetail() {
        return sketchDetail;
    }
}
