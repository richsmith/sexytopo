package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.junit.Assert;

import org.junit.Test;


public class ThExporterTest {


    final String TEST_CONTENT =
            "encoding  utf-8\n" +
                "survey dafung-down-west\n" +
                "\n" +
                "input dafung-down-west.th2\n" +
                "input dafung-down-westEe.th2\n" +
                "\n" +
                "centreline" +
                "\n" +
                "data normal from to tape compass clino ignoreall\n" +
                "  extend right\n" +
                "1.4  8.0  0.0  0.0  0.0  \n" +
                "#8.0  8.1  13.883  283.04  -5.5  \n" +
                "#8.0  8.1  13.885  283.08  -5.5  \n" +
                "#8.0  8.1  13.882  283.08  -5.61  \n" +
                "8.0  8.1  13.883  283.06  -5.54  Calculated leg from 3 above\n" +
                "8.1  -  1.566  106.33  82.08  \n" +
                "endcentreline\n" +
                "endsurvey";

    @Test
    public void testReplaceCentreline() {
        String updated = ThExporter.replaceCentreline(TEST_CONTENT, "replacement");
        Assert.assertTrue(updated.contains("replacement"));
        Assert.assertFalse(updated.contains("Calculated"));
    }


    @Test
    public void testReplaceInputs() {
        String updated = ThExporter.replaceInputsText(TEST_CONTENT, "input replacement");
        Assert.assertTrue(updated.contains("input replacement"));
        Assert.assertFalse(updated.contains("dafung-down-west.th2"));
    }

}
