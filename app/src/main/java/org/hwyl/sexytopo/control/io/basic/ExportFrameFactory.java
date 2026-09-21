package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

public class ExportFrameFactory {

    public static Frame getExportFrame(Survey survey, Projection2D projectionType) {

        // Basic bounds are the union of the sketch and the survey data
        Sketch sketch = survey.getSketch(projectionType);
        Space<Coord2D> projection = projectionType.project(survey);
        Frame sketchBox = Frame.from(sketch);
        Frame surveyDataBox = Space2DUtils.toFrame(projection);
        Frame export = sketchBox.union(surveyDataBox);

        // The sketch's bounding box has each cross-section at its natural size, but they are
        // exported enlarged by the cross-section scale about their positions, so widen the frame
        // to hold them as they are drawn. They are never drawn smaller than the bounding box says,
        // so there is nothing to do unless the scale is above 1.
        float crossSectionScale = sketch.getCrossSectionScale();
        if (crossSectionScale > 1) {
            for (CrossSectionDetail crossSection : sketch.getCrossSectionDetails()) {
                export = export.union(getScaledFrame(crossSection, crossSectionScale));
            }
        }

        return export;
    }

    private static Frame getScaledFrame(CrossSectionDetail crossSection, float scale) {
        Coord2D position = crossSection.getPosition();
        return new Frame(
                scaleAbout(crossSection.getLeft(), position.x, scale),
                scaleAbout(crossSection.getRight(), position.x, scale),
                scaleAbout(crossSection.getTop(), position.y, scale),
                scaleAbout(crossSection.getBottom(), position.y, scale));
    }

    private static float scaleAbout(float value, float centre, float scale) {
        return centre + (value - centre) * scale;
    }

    public static Frame addBorder(Frame export) {
        float largestDimension = Math.max(export.getWidth(), export.getHeight());

        int xPadding = getPadding(export.getWidth());
        int yPadding = getPadding(export.getHeight());

        export = export.addPadding(xPadding, yPadding);

        // Round up to nearest 10m for tidiness; also good for neat grid size etc.
        export = export.expandToNearest(1);

        return export;
    }

    private static int getPadding(float dimension) {
        if (dimension <= 10) {
            return 1;
        } else if (dimension <= 50) {
            return 5;
        } else {
            return 10;
        }
    }
}
