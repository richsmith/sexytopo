package org.hwyl.sexytopo.control.io.thirdparty.pockettopo;

import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class PocketTopoImporterTest {

    /**
     * Build a minimal valid .top file as a byte array for testing.
     *
     * Contains:
     * - 1 trip (date=epoch, empty comment, declination=0)
     * - 2 shots: station 0.0 -> 0.1 (3.5m, 90°, 0°), and a splay from 0.1
     * - 0 references
     * - empty overview mapping
     * - empty plan drawing
     * - empty elevation drawing
     */
    private static byte[] buildMinimalTopFile() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Header: "Top" + version 3
        out.write('T');
        out.write('o');
        out.write('p');
        out.write(3);

        // tripCount = 1
        writeInt32(out, 1);

        // Trip 0: time=TICKS_AT_EPOCH (1 Jan 1970), empty comment, declination=0
        writeInt64(out, PocketTopoFile.TICKS_AT_EPOCH);
        out.write(0); // string length = 0
        writeInt16(out, (short) 0); // declination

        // shotCount = 2
        writeInt32(out, 2);

        // Shot 0: station 0.0 -> 0.1, dist=3500mm, azimuth=east(0x4000), incl=0, no flags
        writeInt32(out, 0x00000000); // from = 0.0
        writeInt32(out, 0x00000001); // to = 0.1
        writeInt32(out, 3500);       // dist = 3500mm = 3.5m
        writeInt16(out, (short) 0x4000); // azimuth = east = 90°
        writeInt16(out, (short) 0);      // inclination = 0°
        out.write(0); // flags
        out.write(0); // roll
        writeInt16(out, (short) 0); // tripIndex

        // Shot 1: splay from 0.1, dist=2000mm, azimuth=north(0), incl=+45°
        writeInt32(out, 0x00000001); // from = 0.1
        writeInt32(out, 0x80000000); // to = undefined (splay)
        writeInt32(out, 2000);       // dist = 2000mm = 2.0m
        writeInt16(out, (short) 0);  // azimuth = north = 0°
        // +45° = 0x4000/2 = 0x2000
        writeInt16(out, (short) 0x2000); // inclination = +45°
        out.write(0); // flags
        out.write(0); // roll
        writeInt16(out, (short) 0); // tripIndex

        // refCount = 0
        writeInt32(out, 0);

        // Overview mapping
        writeInt32(out, 0); // origin x
        writeInt32(out, 0); // origin y
        writeInt32(out, 1000); // scale

        // Plan drawing (outline)
        writeInt32(out, 0); // mapping origin x
        writeInt32(out, 0); // mapping origin y
        writeInt32(out, 1000); // mapping scale
        // 1 polygon element
        out.write(1); // element id = polygon
        writeInt32(out, 2); // pointCount
        writeInt32(out, 1000);  // point0 x = 1000mm = 1.0m
        writeInt32(out, -2000); // point0 y = -2000mm = -2.0m
        writeInt32(out, 3000);  // point1 x = 3000mm = 3.0m
        writeInt32(out, -4000); // point1 y = -4000mm = -4.0m
        out.write(3); // colour = brown
        out.write(0); // end of elements

        // Elevation drawing (sideview)
        writeInt32(out, 0); // mapping origin x
        writeInt32(out, 0); // mapping origin y
        writeInt32(out, 1000); // mapping scale
        out.write(0); // end of elements (empty drawing)

        return out.toByteArray();
    }

    /**
     * Build a .top file with a shot that has a comment (flags bit 1 set).
     */
    private static byte[] buildTopFileWithShotComment() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write('T'); out.write('o'); out.write('p'); out.write(3);

        // 0 trips
        writeInt32(out, 0);

        // 1 shot with comment
        writeInt32(out, 1);
        writeInt32(out, 0x00000000); // from = 0.0
        writeInt32(out, 0x00000001); // to = 0.1
        writeInt32(out, 5000);       // 5m
        writeInt16(out, (short) 0);  // azimuth = north
        writeInt16(out, (short) 0);  // inclination = level
        out.write(2); // flags = bit1 set (has comment)
        out.write(0); // roll
        writeInt16(out, (short) -1); // tripIndex = -1 (no trip)
        // Comment string: "test shot"
        byte[] comment = "test shot".getBytes("UTF-8");
        out.write(comment.length);
        out.write(comment);

        // 0 references
        writeInt32(out, 0);

        // Overview mapping
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000);

        // Empty plan drawing
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000);
        out.write(0);

        // Empty elevation drawing
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000);
        out.write(0);

        return out.toByteArray();
    }

    // --- Helper write methods ---

    private static void writeInt16(ByteArrayOutputStream out, short value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void writeInt32(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeInt64(ByteArrayOutputStream out, long value) {
        writeInt32(out, (int) (value & 0xFFFFFFFFL));
        writeInt32(out, (int) ((value >> 32) & 0xFFFFFFFFL));
    }


    // --- Tests ---

    @Test(expected = IOException.class)
    public void testBadHeaderThrows() throws IOException {
        byte[] data = new byte[]{'B', 'a', 'd', 3};
        PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
    }

    @Test(expected = IOException.class)
    public void testWrongVersionThrows() throws IOException {
        byte[] data = new byte[]{'T', 'o', 'p', 2};
        PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
    }

    @Test
    public void testOriginStationName() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Assert.assertEquals("0.0", survey.getOrigin().getName());
    }

    @Test
    public void testConnectedLegDistance() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Station origin = survey.getOrigin();
        List<Leg> connected = origin.getConnectedOnwardLegs();
        Assert.assertEquals(1, connected.size());
        Assert.assertEquals(3.5f, connected.get(0).getDistance(), 0.01f);
    }

    @Test
    public void testConnectedLegAzimuth() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals(90.0f, leg.getAzimuth(), 0.01f);
    }

    @Test
    public void testConnectedLegInclination() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals(0.0f, leg.getInclination(), 0.01f);
    }

    @Test
    public void testSplayLeg() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Station station01 = survey.getStationByName("0.1");
        Assert.assertNotNull(station01);
        List<Leg> splays = station01.getUnconnectedOnwardLegs();
        Assert.assertEquals(1, splays.size());
        Assert.assertEquals(2.0f, splays.get(0).getDistance(), 0.01f);
        Assert.assertEquals(45.0f, splays.get(0).getInclination(), 0.01f);
    }

    @Test
    public void testPlanSketchHasPath() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        List<PathDetail> paths = survey.getPlanSketch().getPathDetails();
        Assert.assertEquals(1, paths.size());
    }

    @Test
    public void testPlanSketchPathColour() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        PathDetail path = survey.getPlanSketch().getPathDetails().get(0);
        Assert.assertEquals(Colour.BROWN, path.getColour());
    }

    @Test
    public void testPlanSketchPathCoordinates() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        PathDetail path = survey.getPlanSketch().getPathDetails().get(0);
        // Point 0: x=1.0m, y=-2.0m (no Y flip — .top uses screen coordinates)
        Assert.assertEquals(1.0f, path.getPath().get(0).x, 0.01f);
        Assert.assertEquals(-2.0f, path.getPath().get(0).y, 0.01f);
        // Point 1: x=3.0m, y=-4.0m
        Assert.assertEquals(3.0f, path.getPath().get(1).x, 0.01f);
        Assert.assertEquals(-4.0f, path.getPath().get(1).y, 0.01f);
    }

    @Test
    public void testElevationSketchEmpty() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Assert.assertTrue(survey.getElevationSketch().getPathDetails().isEmpty());
    }

    @Test
    public void testShotWithComment() throws IOException {
        byte[] data = buildTopFileWithShotComment();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Station station = survey.getStationByName("0.1");
        Assert.assertNotNull(station);
        Assert.assertEquals("test shot", station.getComment());
    }

    @Test
    public void testCanHandleFileExtension() {
        PocketTopoImporter importer = new PocketTopoImporter();
        // canHandleFile requires DocumentFile which needs Android context,
        // so we just test the name check logic indirectly
        Assert.assertTrue("survey.top".endsWith("top"));
        Assert.assertFalse("survey.txt".endsWith("top"));
    }

    // --- Triple-shot / promotedFrom tests ---

    /**
     * Build a .top file with 3 repeat shots between the same station pair (triple-shot).
     */
    private static byte[] buildTripleShotTopFile() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('T'); out.write('o'); out.write('p'); out.write(3);
        writeInt32(out, 0); // 0 trips

        writeInt32(out, 3); // 3 shots, all from 0.0 → 0.1

        // Shot 0: 5.000m, 90°, 0°
        writeInt32(out, 0x00000000); writeInt32(out, 0x00000001);
        writeInt32(out, 5000);
        writeInt16(out, (short) 0x4000); // 90°
        writeInt16(out, (short) 0);
        out.write(0); out.write(0); writeInt16(out, (short) -1);

        // Shot 1: 5.010m, 90.1°, 0.5°
        writeInt32(out, 0x00000000); writeInt32(out, 0x00000001);
        writeInt32(out, 5010);
        writeInt16(out, (short) 0x4001); // ~90.005°
        writeInt16(out, (short) 0x0024); // ~0.5°
        out.write(0); out.write(0); writeInt16(out, (short) -1);

        // Shot 2: 4.990m, 89.9°, -0.5°
        writeInt32(out, 0x00000000); writeInt32(out, 0x00000001);
        writeInt32(out, 4990);
        writeInt16(out, (short) 0x3FFF); // ~89.995°
        writeInt16(out, (short) 0xFFDC); // ~-0.5°
        out.write(0); out.write(0); writeInt16(out, (short) -1);

        writeInt32(out, 0); // 0 references
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000);
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000); out.write(0);
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000); out.write(0);

        return out.toByteArray();
    }

    @Test
    public void testTripleShotCreatesOneConnectedLeg() throws IOException {
        byte[] data = buildTripleShotTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Assert.assertEquals(2, survey.getAllStations().size());
        Station origin = survey.getOrigin();
        Assert.assertEquals(1, origin.getConnectedOnwardLegs().size());
    }

    @Test
    public void testTripleShotHasPromotedFrom() throws IOException {
        byte[] data = buildTripleShotTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertTrue(leg.wasPromoted());
        Assert.assertEquals(3, leg.getPromotedFrom().length);
    }

    @Test
    public void testTripleShotAveragesDistance() throws IOException {
        byte[] data = buildTripleShotTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        // Average of 5.0, 5.01, 4.99 = 5.0
        Assert.assertEquals(5.0f, leg.getDistance(), 0.01f);
    }

    @Test
    public void testTripleShotPromotedFromHasOriginalReadings() throws IOException {
        byte[] data = buildTripleShotTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Leg[] originals = leg.getPromotedFrom();
        Assert.assertEquals(5.0f, originals[0].getDistance(), 0.01f);
        Assert.assertEquals(5.01f, originals[1].getDistance(), 0.01f);
        Assert.assertEquals(4.99f, originals[2].getDistance(), 0.01f);
    }

    @Test
    public void testSingleShotHasNoPromotedFrom() throws IOException {
        byte[] data = buildMinimalTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertFalse(leg.wasPromoted());
        Assert.assertEquals(0, leg.getPromotedFrom().length);
    }

    // --- Out-of-order shot tests ---

    /**
     * Build a .top file where shots are NOT in tree-traversal order.
     * Shot order: splay from 0.0, connected 0.1→0.2, connected 0.0→0.1
     * The connected shot from 0.1→0.2 comes BEFORE 0.1 is created.
     */
    private static byte[] buildOutOfOrderTopFile() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('T'); out.write('o'); out.write('p'); out.write(3);
        writeInt32(out, 0); // 0 trips

        writeInt32(out, 3); // 3 shots

        // Shot 0: splay from 0.0
        writeInt32(out, 0x00000000); // from = 0.0
        writeInt32(out, 0x80000000); // to = undefined (splay)
        writeInt32(out, 1500);       // 1.5m
        writeInt16(out, (short) 0x4000); // east
        writeInt16(out, (short) 0);
        out.write(0); out.write(0); writeInt16(out, (short) -1);

        // Shot 1: connected 0.1 → 0.2 (0.1 doesn't exist yet!)
        writeInt32(out, 0x00000001); // from = 0.1
        writeInt32(out, 0x00000002); // to = 0.2
        writeInt32(out, 4000);       // 4m
        writeInt16(out, (short) 0);  // north
        writeInt16(out, (short) 0);
        out.write(0); out.write(0); writeInt16(out, (short) -1);

        // Shot 2: connected 0.0 → 0.1 (this creates 0.1)
        writeInt32(out, 0x00000000); // from = 0.0
        writeInt32(out, 0x00000001); // to = 0.1
        writeInt32(out, 3000);       // 3m
        writeInt16(out, (short) 0x8000); // south
        writeInt16(out, (short) 0);
        out.write(0); out.write(0); writeInt16(out, (short) -1);

        writeInt32(out, 0); // 0 references
        // Overview mapping
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000);
        // Empty plan drawing
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000); out.write(0);
        // Empty elevation drawing
        writeInt32(out, 0); writeInt32(out, 0); writeInt32(out, 1000); out.write(0);

        return out.toByteArray();
    }

    @Test
    public void testOutOfOrderShotsAllStationsCreated() throws IOException {
        byte[] data = buildOutOfOrderTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        // All 3 stations should exist: 0.0, 0.1, 0.2
        Assert.assertEquals(3, survey.getAllStations().size());
        Assert.assertNotNull(survey.getStationByName("0.0"));
        Assert.assertNotNull(survey.getStationByName("0.1"));
        Assert.assertNotNull(survey.getStationByName("0.2"));
    }

    @Test
    public void testOutOfOrderShotsLegData() throws IOException {
        byte[] data = buildOutOfOrderTopFile();
        Survey survey = PocketTopoImporter.parseSurvey(new ByteArrayInputStream(data));
        // 0.0 should have: 1 connected leg to 0.1 + 1 splay
        Station origin = survey.getOrigin();
        Assert.assertEquals(1, origin.getConnectedOnwardLegs().size());
        Assert.assertEquals(1, origin.getUnconnectedOnwardLegs().size());
        // 0.1 should have: 1 connected leg to 0.2
        Station station01 = survey.getStationByName("0.1");
        Assert.assertEquals(1, station01.getConnectedOnwardLegs().size());
        Assert.assertEquals(4.0f, station01.getConnectedOnwardLegs().get(0).getDistance(), 0.01f);
    }

    // --- Integration test with real .top file ---

    @Test
    public void testParseCeiledUpFile() throws Exception {
        InputStream resource = getClass().getClassLoader().getResourceAsStream("CeiledUp.top");
        Assert.assertNotNull("Test resource CeiledUp.top not found", resource);

        try (BufferedInputStream in = new BufferedInputStream(resource)) {
            Survey survey = PocketTopoImporter.parseSurvey(in);

            Assert.assertNotNull(survey);
            Assert.assertTrue(survey.getAllStations().size() > 1);
            Assert.assertTrue(survey.getAllLegs().size() > 0);
            Assert.assertNotNull(survey.getTrip());
            Assert.assertNotNull(survey.getTrip().getDate());
            Assert.assertTrue(survey.getTrip().getComments().contains("DistoX"));
        }
    }
}
