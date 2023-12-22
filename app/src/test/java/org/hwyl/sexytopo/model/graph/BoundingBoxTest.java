package org.hwyl.sexytopo.model.graph;

import junit.framework.TestCase;

import org.hwyl.sexytopo.SexyTopoConstants;

public class BoundingBoxTest extends TestCase {

    public void testRounding() {
        BoundingBox box = new BoundingBox(-5, 5, -5, 5);
        BoundingBox rounded = box.roundToNearest(10);
        assertEquals(-10.0, rounded.getLeft(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
        assertEquals(10.0, rounded.getBottom(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }
}