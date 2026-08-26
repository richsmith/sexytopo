package org.hwyl.sexytopo.control.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;

public class TextToolsTest {

    @Test
    public void testAdvanceLastNumber() {
        String advanced = TextTools.advanceLastNumber("S1");
        Assert.assertEquals("S2", advanced);
    }

    @Test
    public void testAdvanceLastNumber2() {
        String advanced = TextTools.advanceLastNumber("S2-1.1");
        Assert.assertEquals("S2-1.2", advanced);
    }

    @Test
    public void testAdvanceLastNumber3() {
        String advanced = TextTools.advanceLastNumber("1");
        Assert.assertEquals("2", advanced);
    }

    @Test
    public void testAdvanceLastNumber4() {
        String advanced = TextTools.advanceLastNumber("foo");
        Assert.assertEquals("foo1", advanced);
    }

    @Test
    public void testAdvanceLastNumber5() {
        String advanced = TextTools.advanceLastNumber("a99f");
        Assert.assertEquals("a100f", advanced);
    }

    @Test
    public void testAdvanceLastNumberWithZeroPadding1() {
        String advanced = TextTools.advanceLastNumber("a01f");
        Assert.assertEquals("a02f", advanced);
    }

    @Test
    public void testAdvanceLastNumberWithZeroPadding2() {
        String advanced = TextTools.advanceLastNumber("a09f");
        Assert.assertEquals("a10f", advanced);
    }

    @Test
    public void testFormatYear() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2024-06-15");
        Assert.assertEquals("2024", TextTools.formatYear(date));
    }

    @Test
    public void testFormatYearAtYearBoundary() throws Exception {
        Date newYearsEve = new SimpleDateFormat("yyyy-MM-dd").parse("2023-12-31");
        Date newYearsDay = new SimpleDateFormat("yyyy-MM-dd").parse("2024-01-01");
        Assert.assertEquals("2023", TextTools.formatYear(newYearsEve));
        Assert.assertEquals("2024", TextTools.formatYear(newYearsDay));
    }
}
