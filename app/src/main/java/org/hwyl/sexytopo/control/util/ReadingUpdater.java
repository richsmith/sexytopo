package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/** Utility class for mutating the individual readings that make up a promoted leg. */
public class ReadingUpdater {

    private ReadingUpdater() {}

    /**
     * Removes the reading at {@code readingIndex} from the promoted leg and re-averages the
     * remaining shots into a new leg. If only one reading remains the result is a plain
     * (non-promoted) leg with an empty {@code promotedFrom}.
     *
     * @param promotedLeg the leg whose readings are being edited
     * @param readingIndex the index of the reading to remove
     * @return the replacement leg
     */
    public static Leg deleteReading(Leg promotedLeg, int readingIndex) {
        List<Leg> remaining = survivingReadings(promotedLeg.getPromotedFrom(), readingIndex);
        return buildReplacementLeg(promotedLeg, remaining);
    }

    /**
     * Removes the reading at {@code readingIndex} from the promoted leg, converts it to a splay,
     * and re-averages the remaining shots into a new leg. If only one reading remains the result
     * leg is a plain (non-promoted) leg.
     *
     * @param promotedLeg the leg whose readings are being edited
     * @param readingIndex the index of the reading to downgrade
     * @return a two-element array: [newPromotedOrPlainLeg, splayLeg]
     */
    public static Leg[] downgradeReading(Leg promotedLeg, int readingIndex) {
        Leg[] readings = promotedLeg.getPromotedFrom();
        Leg splay = readings[readingIndex].toSplay();
        List<Leg> remaining = survivingReadings(readings, readingIndex);
        Leg newLeg = buildReplacementLeg(promotedLeg, remaining);
        return new Leg[] {newLeg, splay};
    }

    /**
     * Replaces the reading at {@code readingIndex} with {@code editedReading} and re-averages all
     * readings into a new promoted leg.
     *
     * @param promotedLeg the leg whose readings are being edited
     * @param readingIndex the index of the reading to replace
     * @param editedReading the replacement reading
     * @return the updated promoted leg
     */
    public static Leg editReading(Leg promotedLeg, int readingIndex, Leg editedReading) {
        Leg[] readings = promotedLeg.getPromotedFrom();
        List<Leg> updated = new ArrayList<>(Arrays.asList(readings));
        updated.set(readingIndex, editedReading);
        return buildReplacementLeg(promotedLeg, updated);
    }

    /**
     * Applies a replacement leg to the survey by delegating to {@link SurveyUpdater#editLeg}, and
     * adds any extra legs (e.g. a splay produced by a downgrade) to the originating station.
     *
     * @param survey the survey to update
     * @param fromStation the station the old leg departs from
     * @param oldLeg the leg being replaced
     * @param newLeg the replacement leg
     * @param extraLegs any additional legs to add to fromStation (e.g. a splay from downgrade)
     */
    public static void applyUpdatedLeg(
            Survey survey, Station fromStation, Leg oldLeg, Leg newLeg, Leg... extraLegs) {
        SurveyUpdater.editLeg(survey, oldLeg, newLeg);
        for (Leg extra : extraLegs) {
            fromStation.addOnwardLeg(extra);
            survey.addLegRecord(extra);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static List<Leg> survivingReadings(Leg[] readings, int removeIndex) {
        List<Leg> survivors = new ArrayList<>(Arrays.asList(readings));
        survivors.remove(removeIndex);
        return survivors;
    }

    private static Leg buildReplacementLeg(Leg promotedLeg, List<Leg> remaining) {
        Leg averaged = SurveyUpdater.averageLegs(remaining);
        Leg[] promotedFrom = remaining.size() > 1 ? remaining.toArray(new Leg[0]) : new Leg[0];
        return new Leg(
                averaged.getDistance(),
                averaged.getAzimuth(),
                averaged.getInclination(),
                promotedLeg.getDestination(),
                promotedFrom,
                promotedLeg.wasShotBackwards());
    }
}
