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
}
