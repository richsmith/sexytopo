package org.hwyl.sexytopo.tests;

import android.test.AndroidTestCase;


import org.hwyl.sexytopo.control.util.TextTools;



public class TextToolsTest extends AndroidTestCase {

    public void testAdvanceLastNumber() {
        String advanced = TextTools.advanceLastNumber("S1");
        assertEquals("S2", advanced);
    }

    public void testAdvanceLastNumber2() {
        String advanced = TextTools.advanceLastNumber("S2-1.1");
        assertEquals("S2-1.2", advanced);
    }

    public void testAdvanceLastNumber3() {
        String advanced = TextTools.advanceLastNumber("1");
        assertEquals("2", advanced);
    }

    public void testAdvanceLastNumber4() {
        String advanced = TextTools.advanceLastNumber("foo");
        assertEquals("foo1", advanced);
    }

    public void testAdvanceLastNumber5() {
        String advanced = TextTools.advanceLastNumber("a99f");
        assertEquals("a100f", advanced);
    }
}
