package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class ExportFrameFactoryTest {

    private static final float DELTA = 1e-4f;

    /**
     * A straight survey with a cross-section on the elevation well out to the right of it, so that
     * the section decides how far right the frame goes. Its east splay is 2m long and it sits at
     * (20, -3).
     */
    private static Survey surveyWithAWideElevationSection(CrossSection.Orientation orientation) {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("2");
        station.addOnwardLeg(new Leg(2, 90, 0));
        survey.getElevationSketch()
                .addCrossSection(
                        CrossSectioner.section(survey, station, orientation), new Coord2D(20, -3));
        return survey;
    }

    @Test
    public void testFrameHoldsEnlargedCrossSections() {
        for (CrossSection.Orientation orientation : CrossSection.Orientation.values()) {
            Survey survey = surveyWithAWideElevationSection(orientation);
            survey.getElevationSketch().setCrossSectionScale(2f);

            Frame frame =
                    ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

            // At twice the size the 2m splay reaches 4m from the section
            Assert.assertEquals(orientation.name(), 24, frame.getRight(), DELTA);
        }
    }

    @Test
    public void testEachFrameOnlyHoldsItsOwnSketchsCrossSections() {
        Survey survey = surveyWithAWideElevationSection(CrossSection.Orientation.VERTICAL);
        survey.getElevationSketch().setCrossSectionScale(3f);

        Frame planFrame = ExportFrameFactory.getExportFrame(survey, Projection2D.PLAN);

        Assert.assertTrue(planFrame.getRight() < 20);
    }
}
