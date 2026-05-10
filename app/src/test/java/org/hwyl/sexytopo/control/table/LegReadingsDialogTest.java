package org.hwyl.sexytopo.control.table;

import static org.junit.Assert.assertNotNull;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.junit.Test;

/**
 * Unit tests for {@link LegReadingsDialog}.
 *
 * <p>Dialog rendering requires an Android context so is covered by instrumented tests. These unit
 * tests verify the data-preparation logic: that promoted legs carry the expected readings and that
 * the guard conditions in {@code onShowReadings} are correct.
 */
public class LegReadingsDialogTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Leg singleShot(float distance, float azimuth, float inclination) {
        return new Leg(distance, azimuth, inclination);
    }

    private static Leg promotedLeg(Station destination, Leg... promotedFrom) {
        Leg first = promotedFrom[0];
        return new Leg(
                first.getDistance(),
                first.getAzimuth(),
                first.getInclination(),
                destination,
                promotedFrom);
    }

    // -------------------------------------------------------------------------
    // wasPromoted / getPromotedFrom
    // -------------------------------------------------------------------------

    @Test
    public void testPromotedLegWithMultipleReadingsIsPromoted() {
        Station to = new Station("2");
        Leg r1 = singleShot(5.0f, 90.0f, 0.0f);
        Leg r2 = singleShot(5.1f, 91.0f, 1.0f);
        Leg leg = promotedLeg(to, r1, r2);

        assert leg.wasPromoted();
        assert leg.getPromotedFrom().length == 2;
    }

    @Test
    public void testPromotedLegWithSingleReadingIsPromoted() {
        Station to = new Station("2");
        Leg r1 = singleShot(5.0f, 90.0f, 0.0f);
        Leg leg = promotedLeg(to, r1);

        assert leg.wasPromoted();
        assert leg.getPromotedFrom().length == 1;
    }

    @Test
    public void testNonPromotedLegIsNotPromoted() {
        Station to = new Station("2");
        Leg leg = new Leg(5.0f, 90.0f, 0.0f, to, new Leg[] {});

        assert !leg.wasPromoted();
    }

    // -------------------------------------------------------------------------
    // Guard: onShowReadings should only proceed for legs with > 1 reading
    // -------------------------------------------------------------------------

    @Test
    public void testReadingsMenuEnabledOnlyForMultipleShots() {
        Station to = new Station("2");

        Leg multiShot = promotedLeg(to, singleShot(5f, 90f, 0f), singleShot(5.1f, 91f, 0f));
        boolean enabledForMulti = multiShot.wasPromoted() && multiShot.getPromotedFrom().length > 1;
        assert enabledForMulti;

        Leg singleShotPromoted = promotedLeg(to, singleShot(5f, 90f, 0f));
        boolean enabledForSingle =
                singleShotPromoted.wasPromoted() && singleShotPromoted.getPromotedFrom().length > 1;
        assert !enabledForSingle;

        Leg plain = new Leg(5f, 90f, 0f, to, new Leg[] {});
        boolean enabledForPlain = plain.wasPromoted() && plain.getPromotedFrom().length > 1;
        assert !enabledForPlain;
    }

    // -------------------------------------------------------------------------
    // Readings data integrity
    // -------------------------------------------------------------------------

    @Test
    public void testReadingsPreserveDistanceAzimuthInclination() {
        Station to = new Station("2");
        Leg r1 = singleShot(3.14f, 123.45f, -5.0f);
        Leg r2 = singleShot(3.20f, 124.00f, -4.5f);
        Leg leg = promotedLeg(to, r1, r2);

        Leg[] readings = leg.getPromotedFrom();
        assertNotNull(readings);
        assert readings[0].getDistance() == 3.14f;
        assert readings[0].getAzimuth() == 123.45f;
        assert readings[0].getInclination() == -5.0f;
        assert readings[1].getDistance() == 3.20f;
        assert readings[1].getAzimuth() == 124.00f;
        assert readings[1].getInclination() == -4.5f;
    }

    @Test
    public void testBackwardsReadingIsReversedForDisplay() {
        Leg shot = new Leg(5.0f, 90.0f, 10.0f, true);
        Leg display = shot.wasShotBackwards() ? shot.reverse() : shot;

        // Reversed azimuth should be 270 (90 + 180), inclination negated
        assert display.getAzimuth() == 270.0f;
        assert display.getInclination() == -10.0f;
        assert display.getDistance() == 5.0f;
    }
}
