package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.junit.Assert;
import org.junit.Test;

public class CrossSectionTest {

    private static final float DELTA = 1e-4f;

    private static Station stationWithSplay(float distance, float azimuth, float inclination) {
        Station station = new Station("A1");
        station.addOnwardLeg(new Leg(distance, azimuth, inclination));
        return station;
    }

    private static Coord2D endOfOnlySplay(CrossSection crossSection) {
        return crossSection.getProjection().getLegMap().values().iterator().next().getEnd();
    }

    private static void assertCoord(float x, float y, Coord2D actual) {
        Assert.assertEquals(x, actual.x, DELTA);
        Assert.assertEquals(y, actual.y, DELTA);
    }

    @Test
    public void testVerticalIsTheDefaultOrientation() {
        CrossSection crossSection = new CrossSection(new Station("A1"), 45f);

        Assert.assertEquals(CrossSection.Orientation.VERTICAL, crossSection.getOrientation());
        Assert.assertTrue(crossSection.isRotatable());
        Assert.assertEquals(45f, crossSection.getAngle(), DELTA);
        Assert.assertEquals(Projection2D.CROSS_SECTION, crossSection.getProjectionType());
    }

    @Test
    public void testHorizontalHasNoAngleAndCannotBeRotated() {
        Station station = new Station("A1");
        CrossSection crossSection = CrossSection.horizontal(station);

        Assert.assertEquals(CrossSection.Orientation.HORIZONTAL, crossSection.getOrientation());
        Assert.assertFalse(crossSection.isRotatable());
        Assert.assertEquals(0f, crossSection.getAngle(), DELTA);
        Assert.assertSame(station, crossSection.getStation());
        Assert.assertEquals(Projection2D.PLAN, crossSection.getProjectionType());
    }

    @Test
    public void testHorizontalProjectionPutsNorthAtTheTop() {
        // Screen y grows downwards, so a splay running north has a negative y
        assertCoord(0, -2, endOfOnlySplay(CrossSection.horizontal(stationWithSplay(2, 0, 0))));
        assertCoord(0, 2, endOfOnlySplay(CrossSection.horizontal(stationWithSplay(2, 180, 0))));
    }

    @Test
    public void testHorizontalProjectionPutsEastOnTheRight() {
        assertCoord(2, 0, endOfOnlySplay(CrossSection.horizontal(stationWithSplay(2, 90, 0))));
        assertCoord(-2, 0, endOfOnlySplay(CrossSection.horizontal(stationWithSplay(2, 270, 0))));
    }

    @Test
    public void testHorizontalProjectionForeshortensInclinedSplays() {
        float expected = (float) (2 * Math.cos(Math.toRadians(30)));
        assertCoord(
                0, -expected, endOfOnlySplay(CrossSection.horizontal(stationWithSplay(2, 0, 30))));
    }

    @Test
    public void testHorizontalProjectionCollapsesVerticalSplays() {
        assertCoord(0, 0, endOfOnlySplay(CrossSection.horizontal(stationWithSplay(2, 0, 90))));
        assertCoord(0, 0, endOfOnlySplay(CrossSection.horizontal(stationWithSplay(2, 0, -90))));
    }

    @Test
    public void testHorizontalProjectionKeepsTheStationAtTheOrigin() {
        Station station = stationWithSplay(2, 90, 0);

        Assert.assertEquals(
                Coord2D.ORIGIN,
                CrossSection.horizontal(station).getProjection().getStationMap().get(station));
    }

    @Test
    public void testHorizontalProjectionIncludesEverySplay() {
        Station station = stationWithSplay(2, 90, 0);
        station.addOnwardLeg(new Leg(3, 0, 0));
        station.addOnwardLeg(new Leg(4, 180, 0));

        Assert.assertEquals(3, CrossSection.horizontal(station).getProjection().getLegMap().size());
    }

    @Test
    public void testVerticalProjectionIsUnchangedFacingNorth() {
        // Facing north, a splay to the east is to the right; one straight up is straight up
        assertCoord(3, 0, endOfOnlySplay(new CrossSection(stationWithSplay(3, 90, 0), 0f)));
        assertCoord(0, -2, endOfOnlySplay(new CrossSection(stationWithSplay(2, 0, 90), 0f)));
    }

    @Test
    public void testVerticalProjectionIsUnchangedFacingEast() {
        // Facing east, a splay to the south is to the right
        assertCoord(3, 0, endOfOnlySplay(new CrossSection(stationWithSplay(3, 180, 0), 90f)));
    }
}
