package org.hwyl.sexytopo.control.io.basic;

import java.util.Random;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class ExportFrameFactoryTest {

    private static final float DELTA = 1e-4f;

    /**
     * A straight survey with a vertical cross-section on the elevation well out to the right of it,
     * so that the section decides how far right the frame goes. Its splays are 2m to the right and
     * 1.5m to the left, and it sits at (20, -3).
     */
    private static Survey surveyWithAWideElevationSection(float crossSectionScale) {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("2");
        station.addOnwardLeg(new Leg(2, 90, 0));
        station.addOnwardLeg(new Leg(1.5f, 270, 0));

        Sketch elevation = survey.getSketch(Projection2D.EXTENDED_ELEVATION);
        elevation.addCrossSection(CrossSectioner.section(survey, station), new Coord2D(20, -3));

        survey.setCrossSectionScale(crossSectionScale);
        return survey;
    }

    @Test
    public void testFrameHoldsACrossSectionAtItsNaturalSize() {
        Survey survey = surveyWithAWideElevationSection(1f);

        Frame frame = ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

        Assert.assertEquals(22, frame.getRight(), DELTA);
    }

    @Test
    public void testFrameGrowsToHoldEnlargedCrossSections() {
        Survey survey = surveyWithAWideElevationSection(2f);

        Frame frame = ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

        // The east splay is 2m long, so at twice the size it reaches 4m from the section
        Assert.assertEquals(24, frame.getRight(), DELTA);
    }

    @Test
    public void testFrameKeepsGrowingAsTheScaleDoes() {
        Survey survey = surveyWithAWideElevationSection(4f);

        Frame frame = ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

        Assert.assertEquals(28, frame.getRight(), DELTA);
    }

    @Test
    public void testFrameGrowsInTheVerticalDirectionToo() {
        Survey survey = surveyWithAWideElevationSection(3f);

        Frame frame = ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

        // A section always reaches at least 1 either side of its position, or 3 at three times
        // the size, and it is centred 3 above the centreline
        Assert.assertEquals(-3 - 3, frame.getTop(), DELTA);
    }

    @Test
    public void testFrameIsNotChangedByASmallerCrossSectionScale() {
        for (float crossSectionScale : new float[] {0.5f, 1f}) {
            Survey survey = surveyWithAWideElevationSection(crossSectionScale);
            Sketch sketch = survey.getSketch(Projection2D.EXTENDED_ELEVATION);
            Frame before =
                    Frame.from(sketch)
                            .union(
                                    Space2DUtils.toFrame(
                                            Projection2D.EXTENDED_ELEVATION.project(survey)));

            Frame after =
                    ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

            String message = "scale " + crossSectionScale;
            Assert.assertEquals(message, before.getLeft(), after.getLeft(), 0f);
            Assert.assertEquals(message, before.getRight(), after.getRight(), 0f);
            Assert.assertEquals(message, before.getTop(), after.getTop(), 0f);
            Assert.assertEquals(message, before.getBottom(), after.getBottom(), 0f);
        }
    }

    @Test
    public void testFrameGrowsToTheLeftAndDownToo() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("2");
        station.addOnwardLeg(new Leg(1.5f, 270, 0));
        survey.getSketch(Projection2D.EXTENDED_ELEVATION)
                .addCrossSection(CrossSectioner.section(survey, station), new Coord2D(-20, 10));
        survey.setCrossSectionScale(3f);

        Frame frame = ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

        // The west splay is 1.5m long, so at three times the size it reaches 4.5m to the left
        Assert.assertEquals(-20 - 4.5f, frame.getLeft(), DELTA);
        // A section reaches at least 1 either side of its position, so 3 at three times the size
        Assert.assertEquals(10 + 3, frame.getBottom(), DELTA);
    }

    @Test
    public void testFrameIsExactlyTheSketchsAtScaleOneWhereverTheSectionsAre() {
        Random random = new Random(1);
        for (int i = 0; i < 50; i++) {
            Survey survey = BasicTestSurveyCreator.createStraightNorth();
            Station station = survey.getStationByName("2");
            station.addOnwardLeg(new Leg(2.3f, 90, 0));
            station.addOnwardLeg(new Leg(1.7f, 270, 0));
            // Awkward positions, so that any rounding in working out the frame would show
            Coord2D position =
                    new Coord2D(30 + random.nextFloat() * 100, 10 + random.nextFloat() * 100);
            Sketch sketch = survey.getSketch(Projection2D.EXTENDED_ELEVATION);
            sketch.addCrossSection(CrossSectioner.section(survey, station), position);
            survey.setCrossSectionScale(1f);
            Frame before =
                    Frame.from(sketch)
                            .union(
                                    Space2DUtils.toFrame(
                                            Projection2D.EXTENDED_ELEVATION.project(survey)));

            Frame after =
                    ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

            String message = "position " + position;
            Assert.assertEquals(message, before.getLeft(), after.getLeft(), 0f);
            Assert.assertEquals(message, before.getRight(), after.getRight(), 0f);
            Assert.assertEquals(message, before.getTop(), after.getTop(), 0f);
            Assert.assertEquals(message, before.getBottom(), after.getBottom(), 0f);
        }
    }

    @Test
    public void testFrameHoldsEnlargedHorizontalCrossSections() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        Frame natural = ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);
        survey.setCrossSectionScale(3f);

        Frame enlarged = ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

        // The horizontal section is at (12, 4) and its east splay is 2m long, so it reaches 6m
        // from there at three times the size
        Assert.assertTrue(enlarged.getRight() >= 12 + 6 - DELTA);
        Assert.assertTrue(enlarged.getRight() > natural.getRight());
    }

    @Test
    public void testPlanFrameHoldsEnlargedCrossSectionsToo() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station station = survey.getStationByName("2");
        station.addOnwardLeg(new Leg(2, 90, 0));
        survey.getSketch(Projection2D.PLAN)
                .addCrossSection(CrossSectioner.section(survey, station), new Coord2D(30, 0));
        survey.setCrossSectionScale(3f);

        Frame frame = ExportFrameFactory.getExportFrame(survey, Projection2D.PLAN);

        Assert.assertEquals(30 + 3 * 2, frame.getRight(), DELTA);
    }

    @Test
    public void testEachFrameOnlyHoldsItsOwnSketchsCrossSections() {
        Survey survey = surveyWithAWideElevationSection(3f);

        Frame planFrame = ExportFrameFactory.getExportFrame(survey, Projection2D.PLAN);

        // The wide section is on the elevation, so it says nothing about how wide the plan is
        Assert.assertTrue(planFrame.getRight() < 20);
    }

    @Test
    public void testFrameHoldsHorizontalAndVerticalSectionsAlike() {
        for (CrossSection.Orientation orientation : CrossSection.Orientation.values()) {
            Survey survey = BasicTestSurveyCreator.createStraightNorth();
            Station station = survey.getStationByName("2");
            station.addOnwardLeg(new Leg(2, 90, 0));
            survey.getSketch(Projection2D.EXTENDED_ELEVATION)
                    .addCrossSection(
                            CrossSectioner.section(survey, station, orientation),
                            new Coord2D(40, 0));
            survey.setCrossSectionScale(2f);

            Frame frame =
                    ExportFrameFactory.getExportFrame(survey, Projection2D.EXTENDED_ELEVATION);

            Assert.assertEquals(orientation.name(), 44, frame.getRight(), DELTA);
        }
    }
}
