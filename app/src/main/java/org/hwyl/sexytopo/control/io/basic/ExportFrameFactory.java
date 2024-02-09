package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

public class ExportFrameFactory {


    public static Frame getExportFrame(
            Survey survey, Projection2D projectionType) {

        // Basic bounds are the union of the sketch and the survey data
        Sketch sketch = survey.getSketch(projectionType);
        Space<Coord2D> projection = projectionType.project(survey);
        Frame sketchBox = Frame.from(sketch);
        Frame surveyDataBox = Space2DUtils.toFrame(projection);
        Frame export = sketchBox.union(surveyDataBox);

        return export;


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
