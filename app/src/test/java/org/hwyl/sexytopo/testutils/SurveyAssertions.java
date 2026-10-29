package org.hwyl.sexytopo.testutils;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;

/** Assertions about the shape of a survey that are shared between tests. */
public class SurveyAssertions {

    /**
     * Splays always run from their station, so they can never be shot backwards, and neither can
     * the readings a leg was promoted from.
     */
    public static void assertNoBackwardSplays(Survey survey) {
        for (Station station : survey.getAllStations()) {
            for (Leg leg : station.getOnwardLegs()) {
                if (!leg.hasDestination()) {
                    Assert.assertFalse(leg.wasShotBackwards());
                }
                for (Leg reading : leg.getPromotedFrom()) {
                    Assert.assertFalse(reading.hasDestination());
                    Assert.assertFalse(reading.wasShotBackwards());
                }
            }
        }
    }
}
