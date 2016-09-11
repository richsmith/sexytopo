package org.hwyl.sexytopo.test;

import android.test.InstrumentationTestCase;

import org.hwyl.sexytopo.control.util.Space3DTransformer;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * Created by rls on 28/07/14.
 */
public class SketchJsonTransformerTest extends InstrumentationTestCase {

    /*
    private static final Coord2D A = new Coord2D(1.0, 1.0);
    private static final Coord2D B = new Coord2D(0.0, 0.0);
    private static final Coord2D C = new Coord2D(0.5, 0.5);
    private static final Coord2D D = new Coord2D(1.0, -1.0);
    private static final Coord2D E = new Coord2D(-5.1, -6.2);
*/

    public void testTransform1MNorth() {
        Leg north1MLeg = new Leg(1, 0, 0);
        Coord3D result = new Space3DTransformer().transform(Coord3D.ORIGIN, north1MLeg);
        Coord3D expected = new Coord3D(0, 1, 0);
        assertEquals(expected, result);
    }

    /*
    public void testCoordTransform() {

        JSONObject json = null;
        assertTrue(true);

        try {
            json = SketchJsonTranslater.toJson(A);
            Coord2D coord2D = SketchJsonTranslater.toCoord2D(json);
            String s = json.toString();
            assertEquals(A, coord2D);
        } catch (JSONException e) {
            e.printStackTrace();
        }



    }*/


}