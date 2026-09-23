package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import org.junit.Assert;
import org.junit.Test;

public class SurveyFormatTest {

    @Test
    public void testSurvexCanAverageRepeatedLegs() {
        Assert.assertTrue(SurveyFormat.SURVEX.canAverageRepeatedLegs());
    }

    @Test
    public void testTherionCannotAverageRepeatedLegsYet() {
        // Therion's own network reduction doesn't yet support treating repeated legs between
        // the same station pair as readings to average - see canAverageRepeatedLegs' javadoc.
        Assert.assertFalse(SurveyFormat.THERION.canAverageRepeatedLegs());
    }
}
