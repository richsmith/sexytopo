package org.hwyl.sexytopo.model.survey;

import org.junit.Assert;
import org.junit.Test;

public class StationTest {
    @Test
    public void testSetNameSanitisesName() {
        Station station = new Station("1");
        station.setName("8\n");
        Assert.assertEquals("8", station.getName());
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
