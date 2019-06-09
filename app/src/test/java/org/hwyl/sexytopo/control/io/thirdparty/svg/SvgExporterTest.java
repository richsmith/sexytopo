package org.hwyl.sexytopo.control.io.thirdparty.svg;

import org.junit.Test;

import java.io.IOException;


public class SvgExporterTest {

    @Test
    public void testSvgHappyPath() throws IOException {
        /* This test not working due to a android testing bug where xmlSerializer is always null
        rather than stubbed :'(
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        SvgExporter svgExporter = new SvgExporter();
        Assert.assertNotNull(testSurvey.getPlanSketch());
        String content = svgExporter.getContent(testSurvey.getPlanSketch());
        Assert.assertTrue(content.contains("<svg"));
        */
    }
}
