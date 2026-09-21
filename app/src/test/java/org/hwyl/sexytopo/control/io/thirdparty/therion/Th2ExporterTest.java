package org.hwyl.sexytopo.control.io.thirdparty.therion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class Th2ExporterTest {

    @Test
    public void testHappyPath() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSections();
        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        exportFrame = exportFrame.scale(scale);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);
        Assert.assertTrue(th2.contains("##XTHERION##"));
    }

    @Test
    public void testCopyrightLineAppearsAfterScrapLineInEveryScrap() {
        // createWithCrossSections() gives us one main "plan" scrap plus two cross-section
        // scraps, so this exercises both getScrap() and getCrossSectionScrap().
        Survey survey = BasicTestSurveyCreator.createWithCrossSections();
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        survey.setTrip(trip);

        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        exportFrame = exportFrame.scale(scale);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);

        String expectedCopyrightLine =
                SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION).trim();

        String[] lines = th2.split("\n");
        int scrapLineCount = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("scrap ")) {
                scrapLineCount++;
                Assert.assertEquals(
                        "Copyright line should immediately follow: " + lines[i],
                        expectedCopyrightLine,
                        lines[i + 1]);
            }
        }
        // One main "plan" scrap + two cross-section scraps
        Assert.assertEquals(3, scrapLineCount);
    }

    @Test
    public void testNoCopyrightLineWhenTripHasNeitherCopyrightNorLicence() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSections();
        // No trip set at all, matching a survey that has never used this feature.

        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        exportFrame = exportFrame.scale(scale);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);

        Assert.assertFalse(th2.contains("copyright"));
    }

    // Cross-sections on the elevation

    // point <x> <y> section -scrap <name>
    private static final Pattern SECTION_POINT =
            Pattern.compile("^point (\\S+) (\\S+) section -scrap (\\S+)");

    // scrap <name> -projection none -scale [0 0 <x> <y> 0 0 10.00 10.00 m]
    private static final Pattern SECTION_SCRAP =
            Pattern.compile("^scrap (\\S+) -projection none -scale \\[0 0 (\\S+) (\\S+) ");

    static String export(Survey survey, Projection2D projection) {
        float scale = TherionExporter.getScale();
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        exportFrame = exportFrame.scale(scale);
        return Th2Exporter.getContent(
                survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);
    }

    /** The regular expression's groups for every line of the th2 that it matches. */
    private static List<List<String>> match(String th2, Pattern pattern) {
        List<List<String>> matches = new ArrayList<>();
        for (String line : th2.split("\n")) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                List<String> groups = new ArrayList<>();
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    groups.add(matcher.group(i));
                }
                matches.add(groups);
            }
        }
        return matches;
    }

    /** The names of the cross-section scraps that the main scrap points at, sorted. */
    static List<String> getSectionPointNames(String th2) {
        List<String> names = new ArrayList<>();
        for (List<String> groups : match(th2, SECTION_POINT)) {
            names.add(groups.get(2));
        }
        Collections.sort(names);
        return names;
    }

    /** The names of the scraps drawn with no projection, as cross-sections are, sorted. */
    static List<String> getSectionScrapNames(String th2) {
        List<String> names = new ArrayList<>();
        for (List<String> groups : match(th2, SECTION_SCRAP)) {
            names.add(groups.get(0));
        }
        Collections.sort(names);
        return names;
    }

    static boolean anyEndWith(List<String> names, String ending) {
        for (String name : names) {
            if (name.endsWith(ending)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testElevationHasAnExtendedScrapAndAScrapForEachCrossSection() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        String th2 = export(survey, Projection2D.EXTENDED_ELEVATION);

        Assert.assertEquals(1, th2.split("-projection extended", -1).length - 1);
        Assert.assertEquals(2, getSectionScrapNames(th2).size());
    }

    @Test
    public void testElevationCrossSectionsArePointedAtByTheMainScrap() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        String th2 = export(survey, Projection2D.EXTENDED_ELEVATION);

        List<String> pointedAt = getSectionPointNames(th2);
        Assert.assertEquals(2, pointedAt.size());
        Assert.assertTrue(anyEndWith(pointedAt, "EEX1"));
        Assert.assertTrue(anyEndWith(pointedAt, "EEX3"));
        // ...and every one of them has the scrap that it points at
        Assert.assertEquals(pointedAt, getSectionScrapNames(th2));
    }

    @Test
    public void testElevationCrossSectionPointsAreWhereTheCrossSectionsAre() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        float scale = TherionExporter.getScale();

        String th2 = export(survey, Projection2D.EXTENDED_ELEVATION);

        for (List<String> point : match(th2, SECTION_POINT)) {
            float x = Float.parseFloat(point.get(0));
            float y = Float.parseFloat(point.get(1));
            // The sections are at (2, 4) and (12, 4), and Therion's y runs the other way
            if (point.get(2).endsWith("EEX1")) {
                Assert.assertEquals(2 * scale, x, 0.01f);
            } else {
                Assert.assertEquals(12 * scale, x, 0.01f);
            }
            Assert.assertEquals(-4 * scale, y, 0.01f);
        }
    }

    @Test
    public void testHorizontalCrossSectionHasANoProjectionScrapLikeAVerticalOne() {
        // Therion has one kind of cross-section scrap, and anything with no projection is one.
        // Station 1 has a vertical cross-section on the elevation and station 3 a horizontal one.
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        String th2 = export(survey, Projection2D.EXTENDED_ELEVATION);

        List<String> scraps = getSectionScrapNames(th2);
        Assert.assertTrue(anyEndWith(scraps, "EEX1"));
        Assert.assertTrue(anyEndWith(scraps, "EEX3"));
    }

    @Test
    public void testPlanAndElevationCrossSectionScrapsHaveDifferentNames() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        List<String> planNames = getSectionScrapNames(export(survey, Projection2D.PLAN));
        List<String> elevationNames =
                getSectionScrapNames(export(survey, Projection2D.EXTENDED_ELEVATION));

        // Stations 1 and 3 have cross-sections on both, and Therion needs every scrap named
        // differently
        Assert.assertEquals(2, planNames.size());
        Assert.assertEquals(2, elevationNames.size());
        for (String name : planNames) {
            Assert.assertFalse(name, elevationNames.contains(name));
        }
    }

    @Test
    public void testEachExportOnlyHasItsOwnSketchsCrossSections() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        survey.getSketch(Projection2D.EXTENDED_ELEVATION)
                .addCrossSection(
                        CrossSection.horizontal(survey.getStationByName("2")), new Coord2D(7, 4));

        String plan = export(survey, Projection2D.PLAN);
        String elevation = export(survey, Projection2D.EXTENDED_ELEVATION);

        Assert.assertTrue(anyEndWith(getSectionPointNames(elevation), "EEX2"));
        Assert.assertEquals(3, getSectionPointNames(elevation).size());
        Assert.assertEquals(2, getSectionPointNames(plan).size());
        Assert.assertFalse(anyEndWith(getSectionPointNames(plan), "2"));
    }

    @Test
    public void testCrossSectionScrapsAreScaledByTheCrossSectionScale() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        float scale = TherionExporter.getScale();

        for (float crossSectionScale : new float[] {1f, 2f, 3f}) {
            survey.setCrossSectionScale(crossSectionScale);

            String th2 = export(survey, Projection2D.EXTENDED_ELEVATION);

            for (List<String> scrap : match(th2, SECTION_SCRAP)) {
                // The scrap is 10m across at the cross-section scale
                float expected = 10 * scale * crossSectionScale;
                Assert.assertEquals(expected, Float.parseFloat(scrap.get(1)), 0.01f);
                Assert.assertEquals(expected, Float.parseFloat(scrap.get(2)), 0.01f);
            }
        }
    }
}
