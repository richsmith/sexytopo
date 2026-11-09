package org.hwyl.sexytopo.model.survey;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class StationTest {
    @Test
    public void testSetNameSanitisesName() {
        Station station = new Station("1");
        station.setName("8\n");
        Assert.assertEquals("8", station.getName());
    }

    private static Leg disabledSplay() {
        Leg splay = new Leg(1, 0, 0);
        splay.setEnabled(false);
        return splay;
    }

    @Test
    public void testOnwardLegsExcludesDisabledSplays() {
        Station station = new Station("1");
        Leg enabledSplay = new Leg(1, 90, 0);
        Leg fullLeg = new Leg(2, 0, 0, new Station("2"), new Leg[] {});
        station.addOnwardLeg(enabledSplay);
        station.addOnwardLeg(disabledSplay());
        station.addOnwardLeg(fullLeg);

        Assert.assertEquals(Arrays.asList(enabledSplay, fullLeg), station.getOnwardLegs());
    }

    @Test
    public void testUnconnectedOnwardLegsExcludesDisabledSplaysAndFullLegs() {
        Station station = new Station("1");
        Leg enabledSplay = new Leg(1, 90, 0);
        station.addOnwardLeg(enabledSplay);
        station.addOnwardLeg(disabledSplay());
        station.addOnwardLeg(new Leg(2, 0, 0, new Station("2"), new Leg[] {}));

        Assert.assertEquals(
                Collections.singletonList(enabledSplay), station.getOnwardLegs(LegFilter.SPLAYS));
    }

    @Test
    public void testDisabledSplaysAreOnlyIncludedWhenAskedFor() {
        Station station = new Station("1");
        Leg disabled = disabledSplay();
        station.addOnwardLeg(disabled);

        Assert.assertTrue(station.getOnwardLegs().isEmpty());
        Assert.assertTrue(station.getOnwardLegs(LegFilter.SPLAYS).isEmpty());
        Assert.assertEquals(
                Collections.singletonList(disabled),
                station.getOnwardLegs(LegFilter.ALL.includeDisabled(true)));
        Assert.assertEquals(
                Collections.singletonList(disabled),
                station.getOnwardLegs(LegFilter.SPLAYS.includeDisabled(true)));
    }

    @Test
    public void testChangingReturnedOnwardLegsDoesNotChangeTheStation() {
        Station station = new Station("1");
        Leg disabled = disabledSplay();
        station.addOnwardLeg(disabled);

        station.getOnwardLegs(LegFilter.ALL.includeDisabled(true)).remove(disabled);

        Assert.assertEquals(1, station.getOnwardLegs(LegFilter.ALL.includeDisabled(true)).size());
    }

    @Test
    public void testRemoveOnwardLegRemovesDisabledSplay() {
        Station station = new Station("1");
        Leg disabled = disabledSplay();
        station.addOnwardLeg(disabled);

        station.removeOnwardLeg(disabled);

        Assert.assertTrue(station.getOnwardLegs(LegFilter.ALL.includeDisabled(true)).isEmpty());
    }

    @Test
    public void testAnonymousStationsAreFoundByNameOnly() {
        Object[][] cases = {
            {"-", Station.ANONYMOUS_WALL},
            {".", Station.ANONYMOUS_FEATURE},
            {"1", null},
            {"0.1", null},
            {"..", null},
            {"", null},
        };
        for (Object[] c : cases) {
            String name = (String) c[0];
            Station expected = (Station) c[1];
            Assert.assertSame(name, expected, Station.getAnonymous(name));
            Assert.assertEquals(name, expected != null, Station.isReservedName(name));
        }
        Assert.assertTrue(Station.ANONYMOUS_WALL.isAnonymous());
        Assert.assertTrue(Station.ANONYMOUS_FEATURE.isAnonymous());
        Assert.assertFalse(new Station("-").isAnonymous());
    }
}
