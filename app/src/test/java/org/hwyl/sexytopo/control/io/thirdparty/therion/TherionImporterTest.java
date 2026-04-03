package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionImporter;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;


public class TherionImporterTest {

    private static final String FAKE_POCKETTOPO_TEXT =
        "encoding  utf-8\n" +
        "\n" +
        "\n" +
        "survey white_lightning -title \"White Lightning\"\n" +
        "input wl.th2\n" +
        "\n" +
        "map wl-main\n" +
        "    wlSP1\n" +
        "    wlSP2\n" +
        "    wlSP3\n" +
        "endmap\n" +
        "\n" +
        "map wl-offset\n" +
        "    wlSP4\n" +
        "endmap\n" +
        "\n" +
        "map map\n" +
        "    wl-main\n" +
        "    break\n" +
        "    wl-offset [-7 12 m] below \n" +
        "    preview below wl-main\n" +
        " endmap\n" +
        "\n" +
        "  \n" +
        "centreline\n" +
        "  date 2015.04.2\n" +
        "  copyright 2015.06.28 \"Rich Smith\"\n" +
        "  author 2015.06.28 \"Rich Smith\"\n" +
        "\n" +
        "  cs EPSG:32651\n" +
        "  # not sure which is the correct station to pin\n" +
        "  # I was just looking at Google Earth with Rufus and trying to \n" +
        "  # remember where the entrance to WL was. \n" +
        "  # Our best guess (and might be way out) is about\n" +
        "  #\n" +
        "  # 18° 1'35.63\"N\n" +
        "  # 122° 1'47.85\"E\n" +
        "\n" +
        "  fix 1.0 397320.00 1993393.00 485\n" +
        "  \n" +
        "  team \"Ruth Allan\" instruments\n" +
        "  team \"Rich Smith\" notes\n" +
        "  team \"Paul Fairman\" dog\n" +
        "  data normal from to compass clino tape\n" +
        //"  extend right\n" +
        "  1.0 - 193.78 0.41 9.118\n" +
        "  1.0 - 328.51 14.60 4.709\n" +
        "  1.0 - 204.27 -77.67 1.572\n" +
        "  1.0 - 246.18 6.41 10.049\n" +
        "  1.0 - 241.99 8.34 13.274\n" +
        "  1.0 1.1 277.13 4.37 10.365\n" +
        "  1.1 - 93.40 21.06 22.044\n" +
        "  1.1 - 102.08 28.62 21.876\n" +
        "  1.1 - 28.82 3.50 3.763\n" +
        "  1.1 - 176.30 7.23 10.188\n" +
        "  1.1 - 280.44 -81.97 1.856\n" +
        "  1.1 - 72.66 71.88 2.772\n" +
        "  1.1 1.2 173.98 -0.46 7.111\n" +
        "  1.2 - 140.92 11.49 0.196\n" +
        "  1.2 - 329.16 -3.80 6.247\n" +
        "  1.2 - 81.27 81.48 10.190\n" +
        "  1.2 - 159.01 -76.51 1.796\n" +
        "  1.2 1.3 237.45 -8.55 10.247\n" +
        "  1.3 - 311.31 9.53 4.209\n" +
        "  1.3 - 137.91 5.90 2.538\n" +
        "  1.3 - 167.27 81.68 9.590\n" +
        "  1.3 - 121.06 -75.02 1.193\n" +
        "  1.3 1.4 166.34 1.62 3.008\n" +
        "  1.4 - 303.29 8.46 5.230\n" +
        "  1.4 - 123.88 10.30 0.168\n" +
        "  1.4 - 299.30 78.33 9.896\n" +
        "  1.4 - 232.73 -67.08 3.201\n" +
        "  1.4 - 295.85 73.19 1.974\n" +
        "  1.4 - 303.62 74.54 9.785\n" +
        "  1.4 - 262.72 -6.45 2.654\n" +
        "  1.4 1.5 236.65 -50.33 17.840\n" +
        "  1.5 - 24.20 2.57 5.221\n" +
        "  1.5 - 206.50 14.33 3.268\n" +
        "  1.5 - 36.46 73.94 14.332\n" +
        "  1.5 - 126.10 -84.75 0.650\n" +
        "  1.5 1.6 294.88 14.17 5.268\n" +
        "  1.6 - 56.00 -7.89 3.199\n" +
        "  1.6 - 336.05 84.30 10.163\n" +
        "  1.6 - 53.13 -83.44 0.350\n" +
        "  1.6 - 318.97 -11.14 4.836\n" +
        "  1.6 - 325.87 -11.03 7.690\n" +
        "  1.6 - 334.70 -9.98 7.589\n" +
        "  1.6 - 356.98 -0.17 5.361\n" +
        "  1.6 1.7 328.82 -16.22 7.531\n" +
        "  1.7 - 237.73 -9.91 3.160\n" +
        "  1.7 - 343.30 -10.53 0.181\n" +
        "  1.7 - 170.42 12.97 2.558\n" +
        "  1.7 - 254.10 77.15 12.591\n" +
        "  1.7 - 258.13 -80.52 1.210\n" +
        "  1.7 - 37.22 12.89 4.381\n" +
        "  1.7 - 61.81 15.44 4.496\n" +
        "  1.7 1.8 179.38 -20.93 2.077\n" +
        "  1.8 - 342.34 -7.62 1.999\n" +
        "  1.8 - 201.06 -10.63 1.399\n" +
        "  1.8 - 295.23 78.99 12.879\n" +
        "  1.8 - 297.26 -83.48 4.536\n" +
        "  1.8 1.9 282.89 -25.54 11.617\n" +
        "  1.9 - 40.41 3.54 0.665\n" +
        "  1.9 - 30.49 7.09 2.090\n" +
        "  1.9 - 236.83 15.04 1.599\n" +
        "  1.9 - 27.75 80.20 15.315\n" +
        "  1.9 - 18.32 -68.12 0.427\n" +
        "  1.9 - 178.51 1.02 3.259\n" +
        "  1.9 - 123.93 2.53 4.460\n" +
        "  1.9 - 110.75 5.77 8.785\n" +
        "  1.9 - 101.65 8.36 10.412\n" +
        "  1.9 - 99.58 6.44 4.057\n" +
        "  1.9 1.10 304.05 5.40 3.125\n" +
        "  1.10 - 23.88 -2.27 1.846\n" +
        "  1.10 - 350.70 83.62 16.076\n" +
        "  1.10 - 313.75 -66.44 2.750\n" +
        "  1.10 - 2.84 -16.34 1.933\n" +
        "  1.10 - 340.20 -21.84 5.345\n" +
        "  1.10 - 316.45 -18.04 5.979\n" +
        "  1.10 - 297.80 -11.09 6.502\n" +
        "  1.10 - 288.66 -9.63 3.317\n" +
        "  1.10 - 334.34 21.74 5.857\n" +
        "  1.10 - 351.18 -4.14 7.876\n" +
        "  1.10 - 353.25 -17.87 12.008\n" +
        "  1.7 - 46.83 48.13 15.259\n" +
        "  1.7 - 70.97 53.54 13.241\n" +
        "  1.7 - 95.06 55.63 9.753\n" +
        "  1.5 1.11 54.01 41.42 12.436\n" +
        "  1.11 - 147.72 4.83 0.226\n" +
        "  1.11 - 300.64 -13.50 1.217\n" +
        "  1.11 - 336.39 83.47 13.979\n" +
        "  1.11 - 279.70 -71.03 2.172\n" +
        "  1.11 1.12 24.84 0.41 4.566\n" +
        "  1.12 - 286.48 15.45 0.424\n" +
        "  1.12 - 124.44 4.55 1.474\n" +
        "  1.12 - 57.53 88.05 0.765\n" +
        "  1.12 - 57.07 -81.08 0.676\n" +
        "  1.12 1.13 37.36 3.89 5.986\n" +
        "  1.13 - 99.04 16.75 0.791\n" +
        "  1.13 - 285.83 6.29 2.198\n" +
        "  1.13 - 302.52 81.98 0.680\n" +
        "  1.13 - 163.30 -81.07 0.471\n" +
        "  1.13 - 321.59 10.18 2.530\n" +
        "  1.13 - 336.96 5.08 4.976\n" +
        "  1.13 - 347.89 4.01 6.815\n" +
        "  1.13 - 353.07 5.11 3.567\n" +
        "  1.13 - 9.15 5.67 2.825\n" +
        "  1.13 - 30.07 4.38 1.806\n" +
        "  1.12 - 88.11 8.38 5.779\n" +
        "  1.12 - 91.11 5.40 6.177\n" +
        "  1.1 1.14 101.24 7.39 12.185\n" +
        "  1.14 - 339.77 -0.47 1.452\n" +
        "  1.14 - 17.81 -70.03 0.594\n" +
        "  1.14 1.15 71.42 44.40 12.181\n" +
        "  1.15 - 110.83 66.65 6.896\n" +
        "\n" +
        "extend start 1.0" +
        "extend right 1.1" +
        "extend left 1.3" +
        "\n" +
        "endcentreline\n" +
        "\n" +
        "\n" +
        "\n" +
        "endsurvey\n" +
        "\n";


    private static final String FAKE_TEXT =
        "survey smugglers-hole -title \"Smugglers' Hole\"\n" +
        "\n" +
        "# input \"smugglers-hole plan.th2\"\n" +
        "input \"smugglers-hole ee.th2\"\n" +
        "  \n" +
        "  centreline\n" +
        "    data normal from to length compass clino\n" +
        "    \n" +
        "    1  -  0.90  333.27  -3.40  \n" +
        "    1  -  1.28  309.47  -6.84  \n" +
        "    1  -  0.95  340.82  -8.40  \n" +
        "    1  -  0.89  332.08  -6.55  \n" +
        "    1  -  0.85  290.91  -78.44  \n" +
        "    1  2  1.70  299.59  -12.32\n" +
        "    2  -  1.54  132.83  12.75  \n" +
        "    2  -  0.76  201.87  6.60  \n" +
        "    2  -  1.14  236.60  1.67  \n" +
        "    2  -  2.26  254.26  9.05  \n" +
        "    2  -  0.44  247.32  89.40  \n" +
        "    2  -  0.64  191.39  -79.58  \n" +
        "    2  -  2.38  183.69  77.33  \n" +
        "    2  -  2.12  280.87  61.80  \n" +
        "    2  -  0.86  182.97  -0.15  \n" +
        "    2  -  1.05  233.93  -7.68  \n" +
        "    2  -  0.91  183.60  -0.33  \n" +
        "    2  -  2.02  239.87  76.25  \n" +
        "    2  -  0.69  198.06  -70.74  \n" +
        "    2  -  1.24  288.54  6.03  \n" +
        "    2  3  2.10  281.21  6.51\n" +
        "    3  -  0.48  175.69  -2.37  \n" +
        "    3  -  0.22  165.14  78.18  \n" +
        "    3  -  0.41  161.39  -79.26  \n" +
        "    3  -  0.51  218.78  22.73  \n" +
        "    3  -  1.76  260.17  19.51  \n" +
        "    3  -  1.28  257.87  15.47  \n" +
        "    3  -  1.29  257.14  14.16  \n" +
        "    3  -  1.29  257.13  14.12  \n" +
        "    3  4  1.48  256.11  10.60\n" +
        "    4  -  1.43  256.72  9.84  \n" +
        "    4  -  0.81  123.11  82.83  \n" +
        "    4  -  0.55  140.91  1.97  \n" +
        "    4  -  0.90  329.39  56.78  \n" +
        "    4  -  0.40  174.32  -78.69  \n" +
        "    4  -  0.47  201.60  12.61  \n" +
        "    4  5  1.38  197.69  0.30\n" +
        "    5  -  0.55  24.52  83.92  \n" +
        "    5  -  0.65  80.13  5.97  \n" +
        "    5  -  1.08  213.87  44.98  \n" +
        "    5  -  1.07  317.24  28.64  \n" +
        "    5  -  0.56  71.08  86.56  \n" +
        "    5  -  1.44  248.19  20.60  \n" +
        "    5  -  1.58  268.63  21.24  \n" +
        "    5  6  2.42  266.72  16.53\n" +
        "    6  -  0.93  266.48  11.46  \n" +
        "    6  -  1.26  249.11  27.41  \n" +
        "    6  -  1.73  219.30  30.65  \n" +
        "    6  -  0.26  181.95  -60.24  \n" +
        "    6  -  0.17  157.31  -11.93  \n" +
        "    6  -  0.55  322.15  -8.35  \n" +
        "    6  -  1.64  234.29  30.10  \n" +
        "    6  -  1.68  235.48  31.16  \n" +
        "    6  -  1.68  235.34  30.92  \n" +
        "    6  -  1.56  233.10  28.80  \n" +
        "    6  7  1.66  235.67  31.07\n" +
        "    7  -  0.55  85.86  78.23  \n" +
        "    7  -  1.07  127.95  30.26  \n" +
        "    7  -  0.87  115.55  16.09  \n" +
        "    7  -  0.49  36.27  75.47  \n" +
        "    7  -  0.24  327.39  34.69  \n" +
        "    7  -  2.37  212.58  55.12  \n" +
        "    7  -  2.37  212.08  55.19  \n" +
        "    7  -  2.36  210.50  55.58  \n" +
        "    7  8  2.14  206.07  52.31\n" +
        "    8  -  0.80  41.03  57.92  \n" +
        "    8  -  1.46  34.58  -0.41  \n" +
        "    8  -  2.35  54.48  -42.81  \n" +
        "    8  -  1.23  28.41  -42.45  \n" +
        "    8  -  1.00  45.99  -84.13  \n" +
        "    8  -  0.99  27.89  59.91  \n" +
        "    8  -  0.60  102.38  1.53  \n" +
        "    8  -  0.85  80.11  -4.15  \n" +
        "    8  -  1.02  359.09  5.27  \n" +
        "    8  -  1.61  39.42  -44.30  \n" +
        "    \n" +
        "    extend start 1\n" +
        "    extend left 2\n" +
        "    extend right 5\n" +
        "  \n" +
        "  endcentreline\n" +
        "endsurvey\n";

    private static final List<String> LINES = Arrays.asList(FAKE_TEXT.split("\n"));

    private static final String APPENDED_STYLE_TEXT =
        "encoding utf-8\n" +
        "survey BentStalAven\n" +
        "# Created with SexyTopo 1.10.1 on 2026-02-04\n" +
        "\n" +
        "date 2026.01.05\n" +
        "#instrument inst \"\"\n" +
        "team \"Will Stuart\" instruments\n" +
        "team \"Andrew Atkinson\" notes\n" +
        "\n" +
        "explo-date 2026.01.05\n" +
        "explo-team \"Will Stuart\"\n" +
        "explo-team \"Andrew Atkinson\"\n" +
        "\n" +
        "centreline\n" +
        "data passage station ignoreall\n" +
        "2\tstn 1 is drilled hole\n" +
        "3\tdrilled hole\n" +
        "\n" +
        "data normal from to tape compass clino ignoreall\n" +
        "1\t-\t1.677\t271.10\t6.59\n" +
        "1\t2\t5.541\t253.93\t4.67\n" +
        "#1\t2\t5.542\t73.95\t-4.64\n" +
        "#1\t2\t5.541\t73.93\t-4.69\n" +
        "#1\t2\t5.541\t73.92\t-4.67\n" +
        "2\t-\t1.371\t140.88\t76.92\n" +
        "2\t3\t2.151\t242.39\t-27.97\n" +
        "#2\t3\t2.151\t62.41\t27.97\n" +
        "#2\t3\t2.152\t62.36\t27.97\n" +
        "#2\t3\t2.151\t62.39\t27.99\n" +
        "3\t-\t1.846\t116.92\t85.17\n" +
        "\n" +
        "extend start 1\n" +
        "endcentreline\n" +
        "endsurvey\n";

    private static final List<String> APPENDED_LINES = Arrays.asList(APPENDED_STYLE_TEXT.split("\n"));

    private static final String INLINE_STYLE_TEXT =
        "encoding utf-8\n" +
        "survey BentStalAven\n" +
        "# Created with SexyTopo 1.9.0 on 2026-01-05\n" +
        "\n" +
        "centreline\n" +
        "date 2026.01.05\n" +
        "team \"Will Stuart\" instruments assistant\n" +
        "team \"Andrew Atkinson\" notes assistant\n" +
        "data normal from to length compass clino\n" +
        "1\t-\t1.677\t271.10\t6.59\n" +
        "2\t1\t5.541\t73.93\t-4.67\t\t#  {from: 5.542 73.95 -4.64, 5.541 73.93 -4.69, 5.541 73.92 -4.67} stn 1 is drilled hole\n" +
        "2\t-\t1.371\t140.88\t76.92\n" +
        "3\t2\t2.151\t62.39\t27.97\t\t#  {from: 2.151 62.41 27.97, 2.152 62.36 27.97, 2.151 62.39 27.99} drilled hole\n" +
        "3\t-\t1.846\t116.92\t85.17\n" +
        "\n" +
        "extend start 1\n" +
        "endcentreline\n" +
        "endsurvey\n";

    private static final List<String> INLINE_LINES = Arrays.asList(INLINE_STYLE_TEXT.split("\n"));

    @Test
    public void testAppendedStyleImport() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(APPENDED_LINES, survey);
        Assert.assertEquals(3, survey.getAllStations().size());

        Station station1 = survey.getStationByName("1");
        Assert.assertNotNull(station1);
        Leg legTo2 = station1.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(legTo2.wasPromoted());
        Assert.assertEquals(3, legTo2.getPromotedFrom().length);
    }

    @Test
    public void testInlineStyleImport() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(INLINE_LINES, survey);
        Assert.assertEquals(3, survey.getAllStations().size());

        Station station1 = survey.getStationByName("1");
        Assert.assertNotNull(station1);
        Leg legTo2 = station1.getConnectedOnwardLegs().get(0);
        Assert.assertTrue(legTo2.wasPromoted());
        Assert.assertEquals(3, legTo2.getPromotedFrom().length);
    }

    @Test
    public void testAppendedStylePassageComments() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(APPENDED_LINES, survey);

        Station station2 = survey.getStationByName("2");
        Assert.assertNotNull(station2.getComment());
        Assert.assertTrue(station2.getComment().contains("stn 1 is drilled hole"));

        Station station3 = survey.getStationByName("3");
        Assert.assertNotNull(station3.getComment());
        Assert.assertTrue(station3.getComment().contains("drilled hole"));
    }

    @Test
    public void testInlineStyleComments() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(INLINE_LINES, survey);

        // Backward leg "2  1  ... # ... stn 1 is drilled hole" puts comment on station 2 (the new one)
        Station station2 = survey.getStationByName("2");
        Assert.assertNotNull(station2.getComment());
        Assert.assertTrue(station2.getComment().contains("stn 1 is drilled hole"));

        // Backward leg "3  2  ... # ... drilled hole" puts comment on station 3 (the new one)
        Station station3 = survey.getStationByName("3");
        Assert.assertNotNull(station3.getComment());
        Assert.assertTrue(station3.getComment().contains("drilled hole"));
    }

    @Test
    public void testBothStylesProduceSameStationCount() throws Exception {
        Survey appendedSurvey = new Survey();
        TherionImporter.updateCentreline(APPENDED_LINES, appendedSurvey);

        Survey inlineSurvey = new Survey();
        TherionImporter.updateCentreline(INLINE_LINES, inlineSurvey);

        Assert.assertEquals(
            appendedSurvey.getAllStations().size(),
            inlineSurvey.getAllStations().size());
    }

    @Test
    public void testBasicBlockExtraction() throws Exception {
        List<String> LINES = Arrays.asList("block", "content", "endblock");
        List<String> output = TherionImporter.getContentsOfBeginEndBlock(LINES, "block");
        Assert.assertEquals(1, output.size());
        Assert.assertEquals("content", output.get(0));
    }

    @Test(expected=Exception.class)
    public void testBlockExtractionFailsIfNoClosingTag() throws Exception {
        List<String> LINES = Arrays.asList("block", "content", "blah");
        TherionImporter.getContentsOfBeginEndBlock(LINES, "block");
    }

    @Test(expected=Exception.class)
    public void testBlockExtractionFailsIfTwoOpeningTags() throws Exception {
        List<String> LINES = Arrays.asList("block", "blah", "block", "blah", "endblock");
        TherionImporter.getContentsOfBeginEndBlock(LINES, "block");
    }


    @Test
    public void testAppendedStyleMetadataImport() throws Exception {
        Trip trip = SurvexTherionImporter.parseMetadata(APPENDED_STYLE_TEXT, SurveyFormat.THERION);
        Assert.assertNotNull(trip);

        // Date should be 2026.01.05
        Assert.assertNotNull(trip.getDate());

        // Instrument commented out — should not be set
        Assert.assertNull(trip.getInstrument());
        Assert.assertFalse(trip.hasInstrument());

        // Team: Will Stuart (instruments) and Andrew Atkinson (notes)
        Assert.assertEquals(2, trip.getTeam().size());
        Assert.assertEquals("Will Stuart", trip.getTeam().get(0).name);
        Assert.assertTrue(trip.getTeam().get(0).roles.contains(Trip.Role.INSTRUMENTS));
        Assert.assertEquals("Andrew Atkinson", trip.getTeam().get(1).name);
        Assert.assertTrue(trip.getTeam().get(1).roles.contains(Trip.Role.BOOK));

        // Explo-team: both should have EXPLORATION role
        Assert.assertTrue(trip.getTeam().get(0).roles.contains(Trip.Role.EXPLORATION));
        Assert.assertTrue(trip.getTeam().get(1).roles.contains(Trip.Role.EXPLORATION));

        // Exploration date same as survey
        Assert.assertTrue(trip.isExplorationDateSameAsSurvey());
    }

    @Test
    public void testInlineStyleMetadataImport() throws Exception {
        Trip trip = SurvexTherionImporter.parseMetadata(INLINE_STYLE_TEXT, SurveyFormat.THERION);
        Assert.assertNotNull(trip);

        // Date should be present
        Assert.assertNotNull(trip.getDate());

        // Team: Will Stuart (instruments, assistant) and Andrew Atkinson (notes, assistant)
        Assert.assertEquals(2, trip.getTeam().size());
        Assert.assertEquals("Will Stuart", trip.getTeam().get(0).name);
        Assert.assertTrue(trip.getTeam().get(0).roles.contains(Trip.Role.INSTRUMENTS));
        Assert.assertEquals("Andrew Atkinson", trip.getTeam().get(1).name);
        Assert.assertTrue(trip.getTeam().get(1).roles.contains(Trip.Role.BOOK));
    }

    @Test
    public void testPocketTopoMetadataImport() throws Exception {
        Trip trip = SurvexTherionImporter.parseMetadata(FAKE_POCKETTOPO_TEXT, SurveyFormat.THERION);
        Assert.assertNotNull(trip);

        // Date: 2015.04.2
        Assert.assertNotNull(trip.getDate());

        // Team: Ruth Allan (instruments), Rich Smith (notes), Paul Fairman (dog)
        Assert.assertEquals(3, trip.getTeam().size());
        Assert.assertEquals("Ruth Allan", trip.getTeam().get(0).name);
        Assert.assertTrue(trip.getTeam().get(0).roles.contains(Trip.Role.INSTRUMENTS));
        Assert.assertEquals("Rich Smith", trip.getTeam().get(1).name);
        Assert.assertTrue(trip.getTeam().get(1).roles.contains(Trip.Role.BOOK));
        Assert.assertEquals("Paul Fairman", trip.getTeam().get(2).name);
        Assert.assertTrue(trip.getTeam().get(2).roles.contains(Trip.Role.DOG));
    }

    @Test
    public void testMetadataImportWithNoMetadata() throws Exception {
        String noMetadata = "centreline\ndata normal from to tape compass clino\n1\t2\t5.0\t0.0\t0.0\nendcentreline\n";
        Trip trip = SurvexTherionImporter.parseMetadata(noMetadata, SurveyFormat.THERION);
        Assert.assertNull(trip);
    }

    @Test
    public void testSurvexMetadataImport() throws Exception {
        String survexText =
            "*begin TestSurvey\n" +
            "*date 2026.01.05\n" +
            "*instrument inst \"DistoX2\"\n" +
            "*team \"Alice\" instruments\n" +
            "*team \"Bob\" notes explorer\n" +
            "\n" +
            "*date explored 2026.01.10\n" +
            "*end TestSurvey\n";

        Trip trip = SurvexTherionImporter.parseMetadata(survexText, SurveyFormat.SURVEX);
        Assert.assertNotNull(trip);
        Assert.assertNotNull(trip.getDate());
        Assert.assertEquals("DistoX2", trip.getInstrument());

        Assert.assertEquals(2, trip.getTeam().size());
        Assert.assertEquals("Alice", trip.getTeam().get(0).name);
        Assert.assertTrue(trip.getTeam().get(0).roles.contains(Trip.Role.INSTRUMENTS));
        Assert.assertEquals("Bob", trip.getTeam().get(1).name);
        Assert.assertTrue(trip.getTeam().get(1).roles.contains(Trip.Role.BOOK));
        Assert.assertTrue(trip.getTeam().get(1).roles.contains(Trip.Role.EXPLORATION));

        // Exploration date is different from survey date
        Assert.assertFalse(trip.isExplorationDateSameAsSurvey());
        Assert.assertNotNull(trip.getExplorationDate());
    }

    @Test
    public void testSurvexCommentedInstrument() throws Exception {
        String survexText =
            "*date 2026.01.05\n" +
            ";*instrument inst \"\"\n";

        Trip trip = SurvexTherionImporter.parseMetadata(survexText, SurveyFormat.SURVEX);
        Assert.assertNotNull(trip);
        Assert.assertNull(trip.getInstrument());
        Assert.assertFalse(trip.hasInstrument());
    }

    // Splays from later stations can appear before the first real leg in
    // chronologically-ordered exports.  The origin must be set from the first
    // non-splay leg so that all stations remain reachable in the survey tree.
    private static final String SPLAYS_BEFORE_LEGS_TEXT =
        "encoding utf-8\n" +
        "survey SplaysFirst\n" +
        "centreline\n" +
        "data normal from to tape compass clino ignoreall\n" +
        "3\t-\t1.000\t90.00\t0.00\n" +    // splay from 3 (not yet connected)
        "3\t-\t2.000\t180.00\t0.00\n" +   // another splay from 3
        "1\t-\t1.500\t270.00\t5.00\n" +   // splay from 1
        "1\t2\t5.000\t100.00\t2.00\n" +   // first real leg → origin should be 1
        "2\t3\t4.000\t200.00\t-3.00\n" +  // connects to 3 (already has splays)
        "endcentreline\n" +
        "endsurvey\n";

    private static final List<String> SPLAYS_BEFORE_LEGS_LINES =
        Arrays.asList(SPLAYS_BEFORE_LEGS_TEXT.split("\n"));

    @Test
    public void testSplaysBeforeLegsOrigin() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(SPLAYS_BEFORE_LEGS_LINES, survey);

        // Origin must be station 1 (first non-splay leg), not station 3 (first splay)
        Assert.assertEquals("1", survey.getOrigin().getName());
    }

    @Test
    public void testSplaysBeforeLegsAllStationsReachable() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(SPLAYS_BEFORE_LEGS_LINES, survey);

        // All 3 stations must be reachable from the origin
        Assert.assertEquals(3, survey.getAllStations().size());
        Assert.assertNotNull(survey.getStationByName("1"));
        Assert.assertNotNull(survey.getStationByName("2"));
        Assert.assertNotNull(survey.getStationByName("3"));
    }

    @Test
    public void testSplaysBeforeLegsPreservesSplays() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(SPLAYS_BEFORE_LEGS_LINES, survey);

        // Station 3's splays (added before connecting leg) must be preserved
        Station station3 = survey.getStationByName("3");
        List<Leg> splays3 = station3.getUnconnectedOnwardLegs();
        Assert.assertEquals(2, splays3.size());

        // Station 1's splay must also be preserved
        Station station1 = survey.getStationByName("1");
        List<Leg> splays1 = station1.getUnconnectedOnwardLegs();
        Assert.assertEquals(1, splays1.size());
    }

    @Test
    public void testChronologicalOrderPreserved() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(SPLAYS_BEFORE_LEGS_LINES, survey);

        // File order: 3 splay, 3 splay, 1 splay, 1→2 leg, 2→3 leg
        List<Leg> chrono = survey.getAllLegsInChronoOrder();
        Assert.assertEquals(5, chrono.size());

        // First two are splays from station 3
        Assert.assertFalse(chrono.get(0).hasDestination());
        Assert.assertFalse(chrono.get(1).hasDestination());

        // Third is splay from station 1
        Assert.assertFalse(chrono.get(2).hasDestination());

        // Fourth is leg 1→2
        Assert.assertTrue(chrono.get(3).hasDestination());
        Assert.assertEquals("2", chrono.get(3).getDestination().getName());

        // Fifth is leg 2→3
        Assert.assertTrue(chrono.get(4).hasDestination());
        Assert.assertEquals("3", chrono.get(4).getDestination().getName());
    }

    @Test
    public void testElevationDirectionExtraction() throws Exception {
        Survey survey = new Survey();
        TherionImporter.updateCentreline(LINES, survey);

        Station stationTwo = survey.getStationByName("2");
        Direction stationTwoDirection = stationTwo.getExtendedElevationDirection();
        Assert.assertEquals(Direction.LEFT, stationTwoDirection);
        Station stationThree = survey.getStationByName("3");
        Direction stationThreeDirection = stationThree.getExtendedElevationDirection();
        Assert.assertEquals(Direction.LEFT, stationThreeDirection);

        Station stationFive = survey.getStationByName("5");
        Direction stationFiveDirection = stationFive.getExtendedElevationDirection();
        Assert.assertEquals(Direction.RIGHT, stationFiveDirection);
    }

}
