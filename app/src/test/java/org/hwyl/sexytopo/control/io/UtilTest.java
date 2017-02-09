package org.hwyl.sexytopo.control.io;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Util.class)
public class UtilTest {

    @Test
    public void testGetNextAvailableNameInitial() throws Exception {
        String name = "foo";

        PowerMockito.spy(Util.class);
        PowerMockito.doReturn(false).when(Util.class, "doesSurveyExist", name + "-2");

        String next = Util.getNextAvailableName(name);
        Assert.assertEquals("foo-2", next);
    }

    @Test
    public void testGetNextAvailableNameSecond() throws Exception {
        String name = "foo";

        PowerMockito.spy(Util.class);
        PowerMockito.doReturn(true).when(Util.class, "doesSurveyExist", name + "-2");
        PowerMockito.doReturn(false).when(Util.class, "doesSurveyExist", name + "-3");

        String next = Util.getNextAvailableName(name);
        Assert.assertEquals("foo-3", next);
    }

    @Test
    public void testGetNextAvailableNameWithExistingNumber() throws Exception {
        String name = "foo19";

        PowerMockito.spy(Util.class);
        PowerMockito.doReturn(false).when(Util.class, "doesSurveyExist", "foo20");

        String next = Util.getNextAvailableName(name);
        Assert.assertEquals("foo20", next);
    }

}
