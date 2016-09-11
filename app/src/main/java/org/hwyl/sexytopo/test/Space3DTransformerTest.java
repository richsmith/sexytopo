package org.hwyl.sexytopo.test;

import android.test.InstrumentationTestCase;

import org.hwyl.sexytopo.control.util.Space3DTransformer;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * Created by rls on 28/07/14.
 */
public class Space3DTransformerTest extends InstrumentationTestCase {


    Space3DTransformer transformer;

    public void testTransform1MNorth() {
        Leg north1MLeg = new Leg(1, 0, 0);
        Coord3D result = transformer.transform(Coord3D.ORIGIN, north1MLeg);
        Coord3D expected = new Coord3D(0, 1, 0);
        assertEquals(expected, result);
    }

    public void testTransform1MEast() {
        Leg east1MLeg = new Leg(1, 90, 0);
        Coord3D result = transformer.transform(Coord3D.ORIGIN, east1MLeg);
        Coord3D expected = new Coord3D(1, 0, 0);
        assertEquals(expected, result);
    }

    public void testTransform1MUp() {
        Leg up1MLeg = new Leg(1, 0, 90);
        Coord3D result = transformer.transform(Coord3D.ORIGIN, up1MLeg);
        Coord3D expected = new Coord3D(0, 0, 1);
        assertEquals(expected, result);
    }

    public void testTransform1MDown() {
        Leg up1MLeg = new Leg(1, 0, -90);
        Coord3D result = transformer.transform(Coord3D.ORIGIN, up1MLeg);
        Coord3D expected = new Coord3D(0, 0, -1);
        assertEquals(expected, result);
    }

    public void testTransform2MNorth() {
        Leg north2MLeg = new Leg(2, 0, 0);
        Coord3D result = transformer.transform(Coord3D.ORIGIN, north2MLeg);
        Coord3D expected = new Coord3D(0, 2, 0);
        assertEquals(expected, result);
    }

    public void testTransform3N3E3S3W() {
        Leg n3 = new Leg(3, 0, 0);
        Coord3D result = transformer.transform(Coord3D.ORIGIN, n3);
        Leg e3 = new Leg(3, 90, 0);
        result = transformer.transform(result, e3);
        Leg s3 = new Leg(3, 180, 0);
        result = transformer.transform(result, s3);
        Leg w3 = new Leg(3, 270, 0);
        result = transformer.transform(result, w3);
        assertEquals(Coord3D.ORIGIN, result);
    }

    public void testTransform10mNEUAndBack() {
        Leg northEastAndUp10M = new Leg(10, 45, 45);
        Coord3D result = transformer.transform(Coord3D.ORIGIN, northEastAndUp10M);
        System.out.println("result = " + result);
        Leg reverse = new Leg(10, 225, -45);
        result = transformer.transform(result, reverse);
        assertEquals(Coord3D.ORIGIN, result);
    }

}