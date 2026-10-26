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

    private static Leg hiddenSplay() {
        Leg splay = new Leg(1, 0, 0);
        splay.setHidden(true);
        return splay;
    }

    @Test
    public void testVisibleOnwardLegsExcludesHiddenSplays() {
        Station station = new Station("1");
        Leg visibleSplay = new Leg(1, 90, 0);
        Leg fullLeg = new Leg(2, 0, 0, new Station("2"), new Leg[] {});
        station.addOnwardLeg(visibleSplay);
        station.addOnwardLeg(hiddenSplay());
        station.addOnwardLeg(fullLeg);

        Assert.assertEquals(Arrays.asList(visibleSplay, fullLeg), station.getVisibleOnwardLegs());
    }

    @Test
    public void testVisibleUnconnectedOnwardLegsExcludesHiddenSplaysAndFullLegs() {
        Station station = new Station("1");
        Leg visibleSplay = new Leg(1, 90, 0);
        station.addOnwardLeg(visibleSplay);
        station.addOnwardLeg(hiddenSplay());
        station.addOnwardLeg(new Leg(2, 0, 0, new Station("2"), new Leg[] {}));

        Assert.assertEquals(
                Collections.singletonList(visibleSplay), station.getVisibleUnconnectedOnwardLegs());
    }

    @Test
    public void testHiddenSplaysStayInOnwardLegs() {
        Station station = new Station("1");
        station.addOnwardLeg(hiddenSplay());

        Assert.assertEquals(1, station.getOnwardLegs().size());
        Assert.assertEquals(1, station.getUnconnectedOnwardLegs().size());
        Assert.assertTrue(station.getVisibleOnwardLegs().isEmpty());
    }
}
