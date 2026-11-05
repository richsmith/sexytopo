package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.Assert;
import org.junit.Test;

public class SurvexTherionImporterDateTest {

    private static final Date EXPECTED = new GregorianCalendar(2026, Calendar.JANUARY, 5).getTime();

    @Test
    public void testAcceptedDatesParseToTheStartDay() {
        String[] inputs = {
            "2026.01.05", // Therion, and Survex before 1.4.13
            "2026-01-05", // ISO, Survex 1.4.13 onwards
            "2026/01/05",
            "2026.1.5",
            "2026.01.05-2026.01.07", // Survex legacy range
            "2026-01-05 2026-01-07", // Survex ISO range
            "2026.01.05 - 2026.01.07", // Therion range
            "2026.01.05@10:30", // Therion time
            "2026.01.05@10:30:15",
            "2026-01-05 ; comment",
        };
        for (String input : inputs) {
            Assert.assertEquals(input, EXPECTED, SurvexTherionImporter.parseDate(input));
        }
    }

    @Test
    public void testPartialImpossibleAndMalformedDatesAreRejected() {
        String[] inputs = {
            // Partial
            "2026",
            "2026.01",
            "2026-01",
            // Impossible
            "2026.13.05",
            "2026.02.30",
            "2026.01.00",
            // Malformed
            "",
            "2026.01-05",
            "05/01/2026",
            "26.01.05",
            "2026.01.050",
            "yyyy.mm.dd",
        };
        for (String input : inputs) {
            Assert.assertNull(input, SurvexTherionImporter.parseDate(input));
        }
    }
}
