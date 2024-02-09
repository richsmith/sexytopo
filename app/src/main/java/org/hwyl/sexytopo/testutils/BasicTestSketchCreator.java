package org.hwyl.sexytopo.testutils;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;


public class BasicTestSketchCreator {

    public static void drawOneHorizontalLine(Sketch sketch) {

        sketch.startNewPath(new Coord2D(5, 0));
        sketch.getActivePath().lineTo(new Coord2D(10, 0));
        sketch.finishPath();
    }

    public static void drawSymbol(Sketch sketch, Symbol symbol) {
        Coord2D coord = new Coord2D(0, 0);
        sketch.addSymbolDetail(coord, symbol, 50, 0);
    }

}
