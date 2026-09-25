package org.hwyl.sexytopo.control.io.thirdparty.svg;

import android.content.SharedPreferences;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.testutils.BasicTestSketchCreator;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@RunWith(RobolectricTestRunner.class)
public class SvgExporterTest {

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
    }

    private static Survey surveyWithTrip(Trip trip) {
        Survey survey = ExampleSurveyCreator.create(5, 2);
        survey.setTrip(trip);
        return survey;
    }

    @Test
    public void testCopyrightAndLicenceAppearInLegendAndDesc() throws Exception {
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("Caver Jane"));
        Assert.assertTrue(content.contains("CC BY 4.0"));
        Assert.assertTrue(content.contains("<desc>"));
        Assert.assertTrue(content.contains("<title>"));
    }

    @Test
    public void testSymbolMarkupSurvivesAndLeavesNoMarkersBehind() throws Exception {
        Survey survey = surveyWithTrip(new Trip());

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        // The sentinels fencing the symbol markup are consumed by the un-escaping, so none of
        // them can reach the file.
        Assert.assertFalse(content.contains("SEXYTOPO_RAW"));
    }

    @Test
    public void testAngleBracketsInCopyrightHolderStayEscaped() throws Exception {
        // Regression test: the whole document used to have &lt;/&gt; rewritten back to raw
        // angle brackets, so a holder like this produced unparseable XML.
        Trip trip = new Trip();
        trip.setCopyrightHolder("Jane <jane@example.com>");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("Jane &lt;jane@example.com&gt;"));
        Assert.assertFalse(content.contains("Jane <jane@example.com>"));

        // Must still parse as XML.
        javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new org.xml.sax.InputSource(new java.io.StringReader(content)));
    }

    @Test
    public void testCopyrightLineWrittenWithoutASurveyDate() throws Exception {
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setSurveyDate(null);
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("Caver Jane"));
    }

    @Test
    public void testCopyrightLineOmittedFromLegendButDescStillPresentWhenOptionDisabled()
            throws Exception {
        SharedPreferences prefs = GeneralPreferences.getRawPreferences();
        Assert.assertNotNull(prefs);
        prefs.edit().putBoolean("pref_export_svg_copyright", false).apply();

        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        // The desc element (non-visual metadata) is independent of the legend toggle, so it
        // should still be present...
        Assert.assertTrue(content.contains("<desc>"));

        // ...but the copyright text should now appear only once (inside <desc>), not a second
        // time as a visible legend line.
        int occurrences = content.split("Caver Jane", -1).length - 1;
        Assert.assertEquals(1, occurrences);
    }

    @Test
    public void testNoTitleOrDescWhenNeitherCopyrightNorLicenceSet() throws Exception {
        Survey survey = ExampleSurveyCreator.create(5, 2); // no trip set at all

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertFalse(content.contains("<desc>"));
        Assert.assertFalse(content.contains("<title>"));
    }

    @Test
    public void testCopyrightOnlyOmitsLicenceText() throws Exception {
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("Caver Jane"));
        Assert.assertTrue(content.contains("\u00A9"));
        Assert.assertFalse(content.contains("CC BY"));
    }

    @Test
    public void testLicenceOnlyOmitsCopyrightSymbol() throws Exception {
        Trip trip = new Trip();
        trip.setLicence("CC BY 4.0");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("CC BY 4.0"));
        Assert.assertFalse(content.contains("\u00A9"));
    }

    @Test
    public void testCopyrightIncludesYearAfterSymbol() throws Exception {
        Trip trip = new Trip();
        trip.setSurveyDate(new SimpleDateFormat("yyyy-MM-dd").parse("2024-06-15"));
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("\u00A9 2024 Caver Jane"));
    }

    @Test
    public void testCopyrightYearOmittedWhenSurveyDateNull() throws Exception {
        Trip trip = new Trip();
        trip.setSurveyDate(null);
        trip.setCopyrightHolder("Caver Jane");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("\u00A9 Caver Jane"));
    }

    // Cross-sections on the elevation

    // What BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation() gives every
    // cross-section: splays 2m east, 1.5m west, 1m north, 1m up and 1m down. Each is listed here as
    // how far it goes across the page and down it, in metres, so the up splay is (0, -1).
    private static final List<String> VERTICAL_SPLAYS =
            sorted("2.00,0.00", "-1.50,0.00", "0.00,0.00", "0.00,-1.00", "0.00,1.00");
    private static final List<String> HORIZONTAL_SPLAYS =
            sorted("2.00,0.00", "-1.50,0.00", "0.00,-1.00", "0.00,0.00", "0.00,0.00");

    private static List<String> sorted(String... vectors) {
        List<String> list = new ArrayList<>(Arrays.asList(vectors));
        Collections.sort(list);
        return list;
    }

    private static Document parse(String svg) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(svg)));
    }

    private static Element findGroup(Document document, String id) {
        NodeList groups = document.getElementsByTagName("g");
        for (int i = 0; i < groups.getLength(); i++) {
            Element group = (Element) groups.item(i);
            if (id.equals(group.getAttribute("id"))) {
                return group;
            }
        }
        return null;
    }

    /** The splays drawn in a cross-section's group, as (across, down) in metres, sorted. */
    private static List<String> getSplays(Document document, String groupId) {
        Element group = findGroup(document, groupId);
        Assert.assertNotNull(groupId + " should be in the export", group);
        List<String> splays = new ArrayList<>();
        NodeList lines = group.getElementsByTagName("polyline");
        for (int i = 0; i < lines.getLength(); i++) {
            String[] points = ((Element) lines.item(i)).getAttribute("points").split(",");
            float across = (Float.parseFloat(points[2]) - Float.parseFloat(points[0])) / 50;
            float down = (Float.parseFloat(points[3]) - Float.parseFloat(points[1])) / 50;
            splays.add(String.format(Locale.US, "%.2f,%.2f", tidy(across), tidy(down)));
        }
        Collections.sort(splays);
        return splays;
    }

    /** The line that a cross-section has drawn in it, as its two ends: x1, y1, x2, y2. */
    private static float[] getDrawnLine(Document document, String groupId) {
        NodeList lines = findGroup(document, groupId).getElementsByTagName("polyline");
        for (int i = 0; i < lines.getLength(); i++) {
            Element line = (Element) lines.item(i);
            // Splays are red; what has been drawn is not
            if (!"red".equals(line.getAttribute("stroke"))) {
                String[] numbers = line.getAttribute("points").split("[ ,]+");
                float[] end = new float[4];
                for (int k = 0; k < 4; k++) {
                    end[k] = Float.parseFloat(numbers[k]);
                }
                return end;
            }
        }
        Assert.fail("Nothing drawn in " + groupId);
        return null;
    }

    // Round off float noise, and turn any negative zero into an ordinary zero
    private static float tidy(float value) {
        return Math.round(value * 100f) / 100f + 0f;
    }

    private static String export(Survey survey, Projection2D projection) throws Exception {
        return new SvgExporter().getContent(survey, projection);
    }

    @Test
    public void testElevationExportIncludesItsCrossSections() throws Exception {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        Document svg = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        Assert.assertNotNull(findGroup(svg, "cross-sections"));
        Assert.assertNotNull(findGroup(svg, "xs-1"));
        Assert.assertNotNull(findGroup(svg, "xs-3"));
    }

    @Test
    public void testVerticalCrossSectionOnTheElevationIsDrawnAcrossThePassage() throws Exception {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        Document svg = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        Assert.assertEquals(VERTICAL_SPLAYS, getSplays(svg, "xs-1"));
    }

    @Test
    public void testHorizontalCrossSectionIsDrawnAsASliceWithNorthAtTheTop() throws Exception {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        Document svg = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        // The north splay goes up the page, and the up and down splays have nothing to show
        Assert.assertEquals(HORIZONTAL_SPLAYS, getSplays(svg, "xs-3"));
    }

    @Test
    public void testTheSameStationCanBeDrawnDifferentlyOnThePlanAndTheElevation() throws Exception {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        Document plan = parse(export(survey, Projection2D.PLAN));
        Document elevation = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        // Station 3 has a vertical cross-section on the plan and a horizontal one on the elevation
        Assert.assertEquals(VERTICAL_SPLAYS, getSplays(plan, "xs-3"));
        Assert.assertEquals(HORIZONTAL_SPLAYS, getSplays(elevation, "xs-3"));
    }

    @Test
    public void testEachExportOnlyHasItsOwnSketchsCrossSections() throws Exception {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        survey.getSketch(Projection2D.EXTENDED_ELEVATION)
                .addCrossSection(
                        CrossSection.horizontal(survey.getStationByName("2")), new Coord2D(7, 4));

        Document plan = parse(export(survey, Projection2D.PLAN));
        Document elevation = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        Assert.assertNotNull(findGroup(elevation, "xs-2"));
        Assert.assertNull(findGroup(plan, "xs-2"));
    }

    @Test
    public void testElevationCrossSectionsAreLeftOutWhenTheOptionIsOff() throws Exception {
        SharedPreferences prefs = GeneralPreferences.getRawPreferences();
        prefs.edit().putBoolean("pref_export_svg_cross_sections", false).apply();
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        Document svg = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        Assert.assertNull(findGroup(svg, "cross-sections"));
        Assert.assertNull(findGroup(svg, "xs-1"));
    }

    @Test
    public void testElevationCrossSectionsAreDrawnAtTheCrossSectionScale() throws Exception {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        survey.getElevationSketch().setCrossSectionScale(2f);

        Document svg = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        Assert.assertEquals(
                sorted("4.00,0.00", "-3.00,0.00", "0.00,0.00", "0.00,-2.00", "0.00,2.00"),
                getSplays(svg, "xs-1"));
        Assert.assertEquals(
                sorted("4.00,0.00", "-3.00,0.00", "0.00,-2.00", "0.00,0.00", "0.00,0.00"),
                getSplays(svg, "xs-3"));
    }

    @Test
    public void testCrossSectionsStayInsideTheImageWhenEnlarged() throws Exception {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        // Large enough for the sections to reach beyond the margin the image leaves around its
        // frame, so that it is the frame that has to make room for them
        survey.getElevationSketch().setCrossSectionScale(6f);

        Document svg = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        String[] box = svg.getDocumentElement().getAttribute("viewBox").split(" ");
        double left = Double.parseDouble(box[0]);
        double top = Double.parseDouble(box[1]);
        double right = left + Double.parseDouble(box[2]);
        double bottom = top + Double.parseDouble(box[3]);

        for (String groupId : new String[] {"xs-1", "xs-3"}) {
            NodeList lines = findGroup(svg, groupId).getElementsByTagName("polyline");
            for (int i = 0; i < lines.getLength(); i++) {
                String[] p = ((Element) lines.item(i)).getAttribute("points").split(",");
                for (int k = 0; k < 4; k += 2) {
                    double x = Double.parseDouble(p[k]);
                    double y = Double.parseDouble(p[k + 1]);
                    String where = groupId + " point " + x + "," + y;
                    Assert.assertTrue(where, x >= left && x <= right);
                    Assert.assertTrue(where, y >= top && y <= bottom);
                }
            }
        }
    }

    @Test
    public void testWhatIsDrawnInAnElevationCrossSectionIsPlacedAndScaledWithIt() throws Exception {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        Sketch elevation = survey.getSketch(Projection2D.EXTENDED_ELEVATION);
        // Each gets a line from (5, 0) to (10, 0) in the cross-section's own drawing
        BasicTestSketchCreator.drawOneHorizontalLine(
                elevation.getCrossSectionDetail(survey.getStationByName("1")).getSketch());
        BasicTestSketchCreator.drawOneHorizontalLine(
                elevation.getCrossSectionDetail(survey.getStationByName("3")).getSketch());

        Document svg = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        // The cross-sections are at (2, 4) and (12, 4), and a metre is 50 units
        Assert.assertArrayEquals(
                new float[] {350, 200, 600, 200}, getDrawnLine(svg, "xs-1"), 0.01f);
        Assert.assertArrayEquals(
                new float[] {850, 200, 1100, 200}, getDrawnLine(svg, "xs-3"), 0.01f);

        survey.getElevationSketch().setCrossSectionScale(2f);
        Document enlarged = parse(export(survey, Projection2D.EXTENDED_ELEVATION));

        // At twice the size the drawing stretches away from where the cross-section is
        Assert.assertArrayEquals(
                new float[] {600, 200, 1100, 200}, getDrawnLine(enlarged, "xs-1"), 0.01f);
        Assert.assertArrayEquals(
                new float[] {1100, 200, 1600, 200}, getDrawnLine(enlarged, "xs-3"), 0.01f);
    }
}
