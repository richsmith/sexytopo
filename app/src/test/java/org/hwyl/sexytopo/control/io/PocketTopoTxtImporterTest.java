package org.hwyl.sexytopo.control.io;

import android.test.AndroidTestCase;

import org.hwyl.sexytopo.control.io.PocketTopoTxtImporter;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Created by rls on 09/11/15.
 */
public class PocketTopoTxtImporterTest {

        public static final String FAKE_TEXT =
            "TRIP\n" +
            "DATE 2005-07-01 \n" +
            "DECLINATION     0.00\n" +
            "DATA\n" +
            "1.0\t\t193.78\t0.41\t9.118\t>\n" +
            "1.0\t\t328.51\t14.60\t4.709\t>\n" +
            "\n" +
            "PLAN\n" +
            "STATIONS\n" +
            "0.000\t0.000\t1.0\n" +
            "-10.255\t1.283\t1.1\n" +
            "SHOTS\n" +
            "1.597\t-1.073\t9.846\t1.700\n" +
            "9.846\t1.700\t12.401\t0.728\n" +
            "POLYLINE BROWN\n" +
            "4.980\t-55.180\n" +
            "POLYLINE RED\n" +
            "3.780\t-48.580\n" +
            "POLYLINE BROWN\n" +
            "6.780\t6.370\n" +
            "7.580\t5.870\n" +
            "\n" +
            "ELEVATION\n" +
            "STATIONS\n" +
            "0.000\t0.000\t1.0\n" +
            "10.335\t0.789\t1.1\n" +
            "SHOTS\n" +
            "22.419\t2.357\t31.121\t10.880\n" +
            "31.121\t10.880\t33.233\t17.211\n" +
            "POLYLINE BLUE\n" +
            "70.600\t-23.300\n" +
            "70.800\t-23.300";

    @Test
    public void testGetSection() {
        String section = PocketTopoTxtImporter.getSection(FAKE_TEXT, "DATA");
        Assert.assertEquals("1.0\t\t193.78\t0.41\t9.118\t>\n1.0\t\t328.51\t14.60\t4.709\t>", section);
    }

    @Test
    public void testGetNamedSubSection() {
        String section = PocketTopoTxtImporter.getSection(FAKE_TEXT, "ELEVATION");
        String stationsSubSection = PocketTopoTxtImporter.getNamedSubSection(section, "STATIONS");
        Assert.assertEquals("0.000\t0.000\t1.0\n10.335\t0.789\t1.1", stationsSubSection);
    }

    @Test
    public void testParsePolylines() {
        String section = PocketTopoTxtImporter.getSection(FAKE_TEXT, "PLAN");
        Set<PathDetail> paths = PocketTopoTxtImporter.parsePolylines(section);

        PathDetail brownPath = null;
        for (PathDetail path : paths) {
            if (path.getColour() == Colour.BROWN) {
                brownPath = path;
            }
        }

        Assert.assertEquals(4.980, brownPath.getPath().get(0).getX(), 0.01);
        Assert.assertEquals(55.180, brownPath.getPath().get(0).getY(), 0.01);

    }
}
