package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.BoundingBox;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;


public class ExportSizeCalculator {

    public static BoundingBox getExportBoundingBox(
            Survey survey, Projection2D projectionType, float scale) {

        // Basic bounds are the union of the sketch and the survey data
        Sketch sketch = survey.getSketch(projectionType);
        Space<Coord2D> projection = projectionType.project(survey);
        return getExportBoundingBox(sketch, projection, scale);
    }

    public static BoundingBox getExportBoundingBox(
            Sketch sketch, Space<Coord2D> projection, float scale) {
        BoundingBox sketchBox = sketch.getBoundingBox();
        Log.d("sketchBox: " + sketchBox.toString());
        BoundingBox surveyDataBox = Space2DUtils.getBoundingBox(projection);
        Log.d("surveyDataBox: " + surveyDataBox.toString());
        BoundingBox combinedBox = sketchBox.union(surveyDataBox);
        Log.d("combinedBox: " + combinedBox.toString());
        BoundingBox scaledBox = combinedBox.scale(scale);
        Log.d("scaledBox: " + scaledBox.toString());

        // Add some padding
        BoundingBox padded;
        float largestDimension = Math.max(scaledBox.getWidth(), scaledBox.getHeight());

        // guessing here what would be a sensible size for the border
        if (largestDimension <= 50) {
            padded = scaledBox.addBorder(5);
        } else {
            padded = scaledBox.addBorder(10);
        }

        Log.d("padded: " + padded.toString());

        // Round up to nearest 10m for tidiness; also good for neat grid size etc.
        BoundingBox rounded = padded.roundToNearest(10);
        Log.d("rounded: " + rounded.toString());

        return rounded;
    }



}
