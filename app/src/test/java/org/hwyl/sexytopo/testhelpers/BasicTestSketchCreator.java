package org.hwyl.sexytopo.testhelpers;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Sketch;


public class BasicTestSketchCreator {

    public static void drawOneHorizontalLine(Sketch sketch) {

        sketch.startNewPath(new Coord2D(5, 0));
        sketch.getActivePath().lineTo(new Coord2D(10, 0));
        sketch.finishPath();
    }

}
