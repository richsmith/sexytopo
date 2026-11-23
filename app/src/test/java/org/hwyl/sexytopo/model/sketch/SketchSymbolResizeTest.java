package org.hwyl.sexytopo.model.sketch;

import java.util.Collections;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Resizing a symbol, which needs its drawable and so the app's resources. */
@RunWith(RobolectricTestRunner.class)
public class SketchSymbolResizeTest {

    private static final float DELTA = 1e-4f;

    @Before
    public void setUp() {
        Symbol.setResources(RuntimeEnvironment.getApplication().getResources());
    }

    @Test
    public void testResizeDetailIsOneUndoableStepKeepingSymbolPlaceAngleAndColour() {
        Sketch sketch = new Sketch();
        sketch.addSymbolDetail(new Coord2D(3, 4), Symbol.STALACTITE, Colour.RED, 2, 45);
        SymbolDetail original = sketch.getSymbolDetails().get(0);

        sketch.resizeDetail(original, 0.5f);

        Assert.assertEquals(1, sketch.getSymbolDetails().size());
        SymbolDetail resized = sketch.getSymbolDetails().get(0);
        Assert.assertEquals(1, resized.getSize(), DELTA);
        Assert.assertEquals(original.getPosition(), resized.getPosition());
        Assert.assertEquals(Symbol.STALACTITE, resized.getSymbol());
        Assert.assertEquals(45, resized.getAngle(), DELTA);
        Assert.assertEquals(Colour.RED, resized.getColour());

        sketch.undo();
        Assert.assertEquals(Collections.singletonList(original), sketch.getSymbolDetails());
    }
}
