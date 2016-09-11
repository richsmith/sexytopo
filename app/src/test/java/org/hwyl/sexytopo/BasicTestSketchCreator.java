package org.hwyl.sexytopo;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;


public class BasicTestSketchCreator {

    public static void drawOneHorizontalLine(Sketch sketch) {

        sketch.startNewPath(new Coord2D(5, 0));
        sketch.getActivePath().lineTo(new Coord2D(10, 0));
        sketch.finishPath();
    }

}
