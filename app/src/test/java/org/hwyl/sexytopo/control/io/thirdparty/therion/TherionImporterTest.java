package org.hwyl.sexytopo.control.io.thirdparty.therion;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;


public class TherionImporterTest {

    String FAKE_TEXT =
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
        "  extend right\n" +
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
        "endcentreline\n" +
        "\n" +
        "\n" +
        "\n" +
        "endsurvey\n" +
        "\n";

    List<String> lines = Arrays.asList(FAKE_TEXT.split("\n"));

    @Test
    public void testBasicBlockExtraction() throws Exception {
        List<String> LINES = Arrays.asList(new String[]{"block", "content", "endblock"});
        List<String> output = TherionImporter.getContentsOfBeginEndBlock(LINES, "block");
        Assert.assertEquals(output.size(), 1);
        Assert.assertEquals(output.get(0), "content");
    }

    @Test(expected=Exception.class)
    public void testBlockExtractionFailsIfNoClosingTag() throws Exception {
        List<String> LINES = Arrays.asList(new String[]{"block", "content", "blah"});
        List<String> output = TherionImporter.getContentsOfBeginEndBlock(LINES, "block");
    }

    @Test(expected=Exception.class)
    public void testBlockExtractionFailsIfTwoOpeningTags() throws Exception {
        List<String> LINES = Arrays.asList(new String[]{"block", "blah", "block", "blah", "endblock"});
        List<String> output = TherionImporter.getContentsOfBeginEndBlock(LINES, "block");
    }

}
