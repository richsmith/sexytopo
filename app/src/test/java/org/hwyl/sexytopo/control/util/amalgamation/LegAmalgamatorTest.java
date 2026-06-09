package org.hwyl.sexytopo.control.util.amalgamation;

import static org.hwyl.sexytopo.SexyTopoConstants.ALLOWED_DOUBLE_DELTA;

import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.model.survey.Leg;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the leg amalgamation strategies. With preferences unset, GeneralPreferences returns the
 * default tolerances (0.05m / 1.7 degrees / 0.05), so these tests exercise the algorithms at their
 * defaults.
 */
public class LegAmalgamatorTest {

    // A pair of essentially-identical steep readings whose azimuths differ wildly. This is the case
    // from issue #321: near the vertical a tiny endpoint variation produces a large azimuth swing.
    private static final List<Leg> STEEP_BUT_SAME =
            Arrays.asList(new Leg(10.0f, 4, 89), new Leg(10.0f, 356, 89));

    @Test
    public void angularRejectsSteepReadingsThatAreReallyTheSame() {
        // The historical behaviour described in the issue: the large azimuth difference causes the
        // angular method to reject these despite them being spatially almost identical.
        Assert.assertFalse(AngularAmalgamator.areReadingsCompatible(STEEP_BUT_SAME));
    }

    @Test
    public void cartesianAcceptsSteepReadingsThatAreReallyTheSame() {
        Assert.assertTrue(CartesianAmalgamator.areReadingsCompatible(STEEP_BUT_SAME));
    }

    @Test
    public void pairwiseAcceptsSteepReadingsThatAreReallyTheSame() {
        Assert.assertTrue(PairwiseAmalgamator.areReadingsCompatible(STEEP_BUT_SAME));
    }

    @Test
    public void cartesianRejectsGenuinelyDifferentReadings() {
        List<Leg> different = Arrays.asList(new Leg(10.0f, 90, 0), new Leg(10.0f, 100, 0));
        Assert.assertFalse(CartesianAmalgamator.areReadingsCompatible(different));
    }

    @Test
    public void cartesianAcceptsCloseFlatReadings() {
        List<Leg> close = Arrays.asList(new Leg(10.0f, 90, 0), new Leg(10.0f, 90.1f, 0));
        Assert.assertTrue(CartesianAmalgamator.areReadingsCompatible(close));
    }

    @Test
    public void pairwiseRejectsGenuinelyDifferentReadings() {
        List<Leg> different = Arrays.asList(new Leg(10.0f, 90, 0), new Leg(10.0f, 100, 0));
        Assert.assertFalse(PairwiseAmalgamator.areReadingsCompatible(different));
    }

    @Test
    public void pairwiseToleranceScalesWithLength() {
        // The same absolute endpoint gap is acceptable on a long leg but not on a short one,
        // because
        // the pairwise error is relative to leg length.
        List<Leg> longLegs = Arrays.asList(new Leg(50.0f, 90, 0), new Leg(50.0f, 90.5f, 0));
        List<Leg> shortLegs = Arrays.asList(new Leg(1.0f, 90, 0), new Leg(1.0f, 110, 0));
        Assert.assertTrue(PairwiseAmalgamator.areReadingsCompatible(longLegs));
        Assert.assertFalse(PairwiseAmalgamator.areReadingsCompatible(shortLegs));
    }

    @Test
    public void angularAveragesComponentsIndependently() {
        List<Leg> legs = Arrays.asList(new Leg(10.0f, 90, 0), new Leg(20.0f, 90, 0));
        Leg averaged = AngularAmalgamator.average(legs);
        Assert.assertEquals(15.0f, averaged.getDistance(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(90.0f, averaged.getAzimuth(), ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(0.0f, averaged.getInclination(), ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void vectorAverageOfSteepReadingsGivesSensibleAzimuth() {
        // Averaging azimuths 4 and 356 as scalars would give a meaningless ~180; the vector average
        // should give roughly 0/360 (north), and the distance and steep inclination should be
        // preserved.
        Leg averaged = CartesianAmalgamator.average(STEEP_BUT_SAME);
        Assert.assertEquals(10.0f, averaged.getDistance(), 0.01f);
        Assert.assertEquals(89.0f, averaged.getInclination(), 0.1f);
        float azimuth = averaged.getAzimuth();
        boolean nearNorth = azimuth < 10 || azimuth > 350;
        Assert.assertTrue("Expected azimuth near north but was " + azimuth, nearNorth);
    }
}
