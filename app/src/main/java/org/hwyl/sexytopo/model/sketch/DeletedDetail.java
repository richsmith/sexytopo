package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;

import java.security.InvalidParameterException;
import java.util.List;


public class DeletedDetail extends SketchDetail {

    private SketchDetail deletedDetail;
    private List<SketchDetail> replacementDetails;

    public DeletedDetail(SketchDetail sketchDetail) {
        super(Colour.BLACK);

        if (sketchDetail instanceof DeletedDetail) {
            throw new InvalidParameterException(
                    "Can't wrap a DeletedDetail in a DeletedDetail");
        }

        this.deletedDetail = sketchDetail;

    }

    public DeletedDetail(SketchDetail sketchDetail, List<SketchDetail> replacementDetails) {
        this(sketchDetail);
        this.replacementDetails = replacementDetails;
    }

    @Override
    public double getDistanceFrom(Coord2D point) {
        return deletedDetail.getDistanceFrom(point);
    }


    @Override
    public SketchDetail translate(Coord2D point) {
        return new DeletedDetail(deletedDetail.translate(point));
    }


    public SketchDetail getDeletedDetail() {
        return deletedDetail;
    }

    public List<SketchDetail> getReplacementDetails() {
        return replacementDetails;
    }
}
