package org.hwyl.sexytopo.control.io;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(IoUtils.class)
public class IoUtilsTest {

    @Test
    public void testGetNextAvailableNameInitial() throws Exception {
        String name = "foo";

        PowerMockito.spy(IoUtils.class);
        PowerMockito.doReturn(false).when(IoUtils.class, "doesSurveyExist", null, name + "-2");

        String next = IoUtils.getNextAvailableName(null, name);
        Assert.assertEquals("foo-2", next);
    }

    @Test
    public void testGetNextAvailableNameSecond() throws Exception {
        String name = "foo";

        PowerMockito.spy(IoUtils.class);
        PowerMockito.doReturn(true).when(IoUtils.class, "doesSurveyExist", null, name + "-2");
        PowerMockito.doReturn(false).when(IoUtils.class, "doesSurveyExist", null, name + "-3");

        String next = IoUtils.getNextAvailableName(null, name);
        Assert.assertEquals("foo-3", next);
    }

    @Test
    public void testGetNextAvailableNameWithExistingNumber() throws Exception {
        String name = "foo19";

        PowerMockito.spy(IoUtils.class);
        PowerMockito.doReturn(false).when(IoUtils.class, "doesSurveyExist", null, "foo20");

        String next = IoUtils.getNextAvailableName(null, name);
        Assert.assertEquals("foo20", next);
    }

}
