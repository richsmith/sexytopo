package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.LegFilter;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

public class GraphToListTranslator {

    public List<SurveyListEntry> toListOfSurveyListEntries(Survey survey) {
        return createListOfEntriesFromStation(survey.getOrigin(), LegFilter.ALL);
    }

    /** The enabled legs and splays of the survey, in the order they were taken. */
    public List<SurveyListEntry> toChronoListOfSurveyListEntries(Survey survey) {
        return toChronoListOfSurveyListEntries(survey, LegFilter.ALL);
    }

    /**
     * The legs and splays of the survey that match the filter, in the order they were taken. The
     * data table and some exports use LegFilter.ALL.includeDisabled(true), as they show disabled
     * splays.
     */
    public List<SurveyListEntry> toChronoListOfSurveyListEntries(Survey survey, LegFilter filter) {
        final List<Leg> chronoLegs = survey.getAllLegsInChronoOrder();
        List<SurveyListEntry> entries = createListOfEntriesFromStation(survey.getOrigin(), filter);
        Collections.sort(
                entries, (e0, e1) -> chronoLegs.indexOf(e0.leg) - chronoLegs.indexOf(e1.leg));
        return entries;
    }

    private List<SurveyListEntry> createListOfEntriesFromStation(Station from, LegFilter filter) {

        List<SurveyListEntry> list = new ArrayList<>();

        for (Leg leg : from.getOnwardLegs(filter)) {
            if (!leg.hasDestination()) {
                list.add(new SurveyListEntry(from, leg));
            }
        }

        // The whole survey is walked, whatever the filter, so every station is reached
        for (Leg leg : from.getOnwardLegs(LegFilter.LEGS)) {
            if (filter.test(leg)) {
                list.add(new SurveyListEntry(from, leg));
            }
            list.addAll(createListOfEntriesFromStation(leg.getDestination(), filter));
        }

        return list;
    }

    public static Map<TableCol, Object> createMap(SurveyListEntry entry) {

        AsTakenReading reading = toAsTakenReading(entry);
        Leg leg = reading.getLeg();

        Map<TableCol, Object> map = new HashMap<>();
        map.put(TableCol.FROM, reading.getFrom());
        map.put(TableCol.TO, reading.getTo());
        map.put(TableCol.DISTANCE, leg.getDistance());
        map.put(TableCol.AZIMUTH, leg.getAzimuth());
        map.put(TableCol.INCLINATION, leg.getInclination());

        return map;
    }

    /**
     * Returns the from-station, to-station, and leg reading exactly as they were physically taken.
     *
     * <p>A leg with {@code wasShotBackwards() == true} is stored with its azimuth/inclination as
     * read at the far station, but attached to the graph in the opposite direction to how it was
     * shot. This normalises that back to the as-taken from/to/azimuth/inclination, so every output
     * (table display, third-party format export, etc) shows and exports the same reading a surveyor
     * record in their notes.
     */
    public static AsTakenReading toAsTakenReading(SurveyListEntry entry) {
        Station from = entry.getFrom();
        Leg leg = entry.getLeg();

        if (leg.wasShotBackwards()) {
            Station to = leg.getDestination();
            return new AsTakenReading(to, from, leg.asBacksight());
        } else {
            return new AsTakenReading(from, leg.getDestination(), leg);
        }
    }

    public static class AsTakenReading {
        private final Station from;
        private final Station to;
        private final Leg leg;

        public AsTakenReading(Station from, Station to, Leg leg) {
            this.from = from;
            this.to = to;
            this.leg = leg;
        }

        public Station getFrom() {
            return from;
        }

        public Station getTo() {
            return to;
        }

        public Leg getLeg() {
            return leg;
        }
    }

    public static class SurveyListEntry {
        private final Station from;
        private final Leg leg;

        public SurveyListEntry(Station from, Leg leg) {
            this.from = from;
            this.leg = leg;
        }

        public Station getFrom() {
            return from;
        }

        public Leg getLeg() {
            return leg;
        }
    }
}
