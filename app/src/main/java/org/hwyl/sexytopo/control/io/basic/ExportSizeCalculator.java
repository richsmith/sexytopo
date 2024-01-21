package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

public class ExportSizeCalculator {


    public static Frame getExportFrame(
            Survey survey, Projection2D projectionType) {

        // Basic bounds are the union of the sketch and the survey data
        Sketch sketch = survey.getSketch(projectionType);
        Space<Coord2D> projection = projectionType.project(survey);
        return getExportFrame(sketch, projection);
    }

    public static Frame getExportFrame(
            Sketch sketch, Space<Coord2D> projection) {
        Frame sketchBox = Frame.from(sketch);
        Frame surveyDataBox = Space2DUtils.toFrame(projection);
        Frame export = sketchBox.union(surveyDataBox);

        // Add some padding
        float largestDimension = Math.max(export.getWidth(), export.getHeight());

        // guessing here what would be a sensible size for the border
        if (largestDimension <= 10) {
            export = export.addPadding(1);
        } else if (largestDimension <= 50) {
            export = export.addPadding(5);
        } else {
            export = export.addPadding(10);
        }

        // Round up to nearest 10m for tidiness; also good for neat grid size etc.
        export = export.expandToNearest(10);

        return export;
    }


}
