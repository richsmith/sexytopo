package org.hwyl.sexytopo.control.table;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for EditLegForm. Note: These tests focus on the core leg creation logic without UI
 * dependencies.
 */
public class EditLegFormTest {

    private static final float DELTA = 0.001f;

    @Test
    public void testNewLegCreationDoesNotRequireDestination() {
        // Simulate creating a new leg with measurements but no destination yet
        float distance = 5.0f;
        float azimuth = 45.0f;
        float inclination = 10.0f;

        // This is what happens when adding a new station manually:
        // 1. Create a leg without destination (should work)
        Leg legWithoutDest = new Leg(distance, azimuth, inclination);
        Assert.assertNotNull(legWithoutDest);
        Assert.assertFalse(legWithoutDest.hasDestination());

        // 2. Then caller creates the destination and reconstructs the leg
        Station newStation = new Station("2");
        Leg legWithDest = new Leg(distance, azimuth, inclination, newStation, new Leg[] {});
        Assert.assertTrue(legWithDest.hasDestination());
        Assert.assertEquals(newStation, legWithDest.getDestination());
    }

    /** Test that splays can be created without destinations. */
    @Test
    public void testSplayCreationWithoutDestination() {
        Leg splay = new Leg(3.5f, 90.0f, 0.0f);
        Assert.assertNotNull(splay);
        Assert.assertFalse(splay.hasDestination());
    }

    @Test
    public void testCannotCreateLegWithNullDestinationDirectly() {
        try {
            // Trying to create a full leg (non-splay) with null destination should fail
            new Leg(5.0f, 0.0f, 0.0f, null, new Leg[] {});
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected - destination should not be null for full legs
            Assert.assertTrue(e.getMessage().contains("Destination"));
        }
    }

    // ---- decomposeToDms tests ----

    @Test
    public void testDmsDecompositionPositive() {
        // 45.5° = 45° 30' 0"
        float[] dms = Leg.decomposeToDms(45.5f);
        Assert.assertEquals(45, (int) dms[0]);
        Assert.assertEquals(30, (int) dms[1]);
        Assert.assertEquals(0.0f, dms[2], DELTA);
    }

    @Test
    public void testDmsDecompositionNegative() {
        // -30.75° = -30° 45' 0"  (sign on degrees only, minutes/seconds non-negative)
        float[] dms = Leg.decomposeToDms(-30.75f);
        Assert.assertEquals(-30, (int) dms[0]);
        Assert.assertEquals(45, (int) dms[1]);
        Assert.assertEquals(0.0f, dms[2], DELTA);
    }

    @Test
    public void testDmsDecompositionZero() {
        float[] dms = Leg.decomposeToDms(0.0f);
        Assert.assertEquals(0, (int) dms[0]);
        Assert.assertEquals(0, (int) dms[1]);
        Assert.assertEquals(0.0f, dms[2], DELTA);
    }

    @Test
    public void testDmsDecompositionRoundTripPositive() {
        float original = 47.3f;
        float[] dms = Leg.decomposeToDms(original);
        float sign = dms[0] < 0 ? -1.0f : 1.0f;
        float recomposed = dms[0] + sign * (dms[1] / 60.0f + dms[2] / 3600.0f);
        Assert.assertEquals(original, recomposed, DELTA);
    }

    @Test
    public void testDmsDecompositionRoundTripNegative() {
        float original = -23.6f;
        float[] dms = Leg.decomposeToDms(original);
        float sign = dms[0] < 0 ? -1.0f : 1.0f;
        float recomposed = dms[0] + sign * (dms[1] / 60.0f + dms[2] / 3600.0f);
        Assert.assertEquals(original, recomposed, DELTA);
    }

    @Test
    public void testDmsDecompositionAtPlusNinety() {
        // +90° = 90° 0' 0"
        float[] dms = Leg.decomposeToDms(90.0f);
        Assert.assertEquals(90, (int) dms[0]);
        Assert.assertEquals(0, (int) dms[1]);
        Assert.assertEquals(0.0f, dms[2], DELTA);
    }

    @Test
    public void testDmsDecompositionAtMinusNinety() {
        // -90° = -90° 0' 0"
        float[] dms = Leg.decomposeToDms(-90.0f);
        Assert.assertEquals(-90, (int) dms[0]);
        Assert.assertEquals(0, (int) dms[1]);
        Assert.assertEquals(0.0f, dms[2], DELTA);
    }
}
