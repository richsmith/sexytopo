package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** How the preferences affect the Therion export. */
@RunWith(RobolectricTestRunner.class)
public class Th2ExporterPreferencesTest {

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
        prefs = GeneralPreferences.getRawPreferences();
        Assert.assertNotNull(prefs);
    }

    @Test
    public void testElevationCrossSectionNamesFollowTheElevationSuffixPreference() {
        prefs.edit().putString("pref_therion_ee_xs_suffix", "SEC-#").apply();
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        List<String> elevation =
                Th2ExporterTest.getSectionPointNames(
                        Th2ExporterTest.export(survey, Projection2D.EXTENDED_ELEVATION));
        List<String> plan =
                Th2ExporterTest.getSectionPointNames(
                        Th2ExporterTest.export(survey, Projection2D.PLAN));

        Assert.assertTrue(Th2ExporterTest.anyEndWith(elevation, "SEC-1"));
        Assert.assertTrue(Th2ExporterTest.anyEndWith(elevation, "SEC-3"));
        // The plan has its own preference, which hasn't been changed
        Assert.assertTrue(Th2ExporterTest.anyEndWith(plan, "PX1"));
    }

    @Test
    public void testPlanCrossSectionNamesFollowThePlanSuffixPreference() {
        prefs.edit().putString("pref_therion_plan_xs_suffix", "S#").apply();
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        List<String> plan =
                Th2ExporterTest.getSectionPointNames(
                        Th2ExporterTest.export(survey, Projection2D.PLAN));
        List<String> elevation =
                Th2ExporterTest.getSectionPointNames(
                        Th2ExporterTest.export(survey, Projection2D.EXTENDED_ELEVATION));

        Assert.assertTrue(Th2ExporterTest.anyEndWith(plan, "S1"));
        Assert.assertTrue(Th2ExporterTest.anyEndWith(elevation, "EEX1"));
    }

    @Test
    public void testNoCrossSectionsAreExportedWhenTheOptionIsOff() {
        prefs.edit().putBoolean("pref_therion_cross_sections", false).apply();
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        for (Projection2D projection :
                new Projection2D[] {Projection2D.PLAN, Projection2D.EXTENDED_ELEVATION}) {
            String th2 = Th2ExporterTest.export(survey, projection);

            Assert.assertTrue(
                    projection.name(), Th2ExporterTest.getSectionPointNames(th2).isEmpty());
            Assert.assertTrue(
                    projection.name(), Th2ExporterTest.getSectionScrapNames(th2).isEmpty());
        }
    }

    @Test
    public void testLinesArePointByPointByDefault() {
        String wall = getExportedLine(LineType.WALL);
        for (String row : getPointRows(wall)) {
            Assert.assertEquals(row, 2, row.split(" ").length);
        }
    }

    @Test
    public void testLinesAreCurvesWhenTheOptionIsOn() {
        prefs.edit().putBoolean("pref_therion_export_line_curves", true).apply();
        String wall = getExportedLine(LineType.WALL);
        List<String> rows = getPointRows(wall);

        // a start point, then fewer curves than there were drawn points
        Assert.assertEquals(2, rows.get(0).split(" ").length);
        Assert.assertTrue(rows.size() > 1 && rows.size() < getCurvedWall().size());
        for (String row : rows.subList(1, rows.size())) {
            Assert.assertEquals(row, 6, row.split(" ").length);
        }
    }

    @Test
    public void testCurvedSlopeKeepsItsLineSize() {
        prefs.edit().putBoolean("pref_therion_export_line_curves", true).apply();
        String slope = getExportedLine(LineType.SLOPE);
        String[] rows = slope.split("\n");
        Assert.assertTrue(rows[2].trim().startsWith("l-size "));
    }

    private static List<Coord2D> getCurvedWall() {
        List<Coord2D> points = new ArrayList<>();
        for (int i = 0; i <= 30; i++) {
            double angle = Math.PI * i / 30;
            points.add(new Coord2D((float) (5 * Math.cos(angle)), (float) (5 * Math.sin(angle))));
        }
        return points;
    }

    private static String getExportedLine(LineType lineType) {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        List<PathDetail> pathDetails = new ArrayList<>();
        pathDetails.add(new PathDetail(getCurvedWall(), Colour.BLACK, lineType));
        survey.getPlanSketch().setPathDetails(pathDetails);

        String th2 = Th2ExporterTest.export(survey, Projection2D.PLAN);
        String start = "line " + lineType.getTherionName() + "\n";
        int from = th2.indexOf(start);
        Assert.assertTrue(from >= 0);
        return th2.substring(from, th2.indexOf("endline", from));
    }

    /** The rows of coordinates in a line, leaving out the line command and any options. */
    private static List<String> getPointRows(String line) {
        List<String> rows = new ArrayList<>(Arrays.asList(line.trim().split("\n")));
        rows.remove(0);
        rows.removeIf(row -> row.trim().startsWith("l-size"));
        rows.replaceAll(String::trim);
        return rows;
    }
}
