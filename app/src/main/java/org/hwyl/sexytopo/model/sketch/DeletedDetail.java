package org.hwyl.sexytopo.model.sketch;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * Undo history entry recording that one or more details were removed from the sketch, optionally
 * replaced by others (e.g. the surviving fragments of a partially-erased path, or the merged result
 * of overlapping areas).
 */
public class DeletedDetail extends SketchDetail {

    private final List<SketchDetail> deletedDetails;
    private List<SketchDetail> replacementDetails = new ArrayList<>();

    public DeletedDetail(SketchDetail sketchDetail) {
        this(Collections.singletonList(sketchDetail));
    }

    public DeletedDetail(List<SketchDetail> sketchDetails) {
        super(Colour.BLACK);

        for (SketchDetail sketchDetail : sketchDetails) {
            if (sketchDetail instanceof DeletedDetail) {
                throw new InvalidParameterException(
                        "Can't wrap a DeletedDetail in a DeletedDetail");
            }
        }

        this.deletedDetails = sketchDetails;
    }

    public DeletedDetail(SketchDetail sketchDetail, List<SketchDetail> replacementDetails) {
        this(sketchDetail);
        this.replacementDetails = replacementDetails;
    }

    public DeletedDetail(List<SketchDetail> sketchDetails, List<SketchDetail> replacementDetails) {
        this(sketchDetails);
        this.replacementDetails = replacementDetails;
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        float minDistance = Float.MAX_VALUE;
        for (SketchDetail deletedDetail : deletedDetails) {
            minDistance = Math.min(minDistance, deletedDetail.getDistanceFrom(point));
        }
        return minDistance;
    }

    @Override
    public SketchDetail translate(Coord2D translation) {
        List<SketchDetail> translated = new ArrayList<>();
        for (SketchDetail deletedDetail : deletedDetails) {
            translated.add(deletedDetail.translate(translation));
        }
        return new DeletedDetail(translated);
    }

    @Override
    public SketchDetail scale(float scale) {
        List<SketchDetail> scaled = new ArrayList<>();
        for (SketchDetail deletedDetail : deletedDetails) {
            scaled.add(deletedDetail.scale(scale));
        }
        return new DeletedDetail(scaled);
    }

    public List<SketchDetail> getDeletedDetails() {
        return deletedDetails;
    }

    public List<SketchDetail> getReplacementDetails() {
        return replacementDetails;
    }
}
