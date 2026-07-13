package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

public class GraphToListTranslator {

    public List<SurveyListEntry> toListOfSurveyListEntries(Survey survey) {
        return createListOfEntriesFromStation(survey.getOrigin());
    }

    public List<SurveyListEntry> toChronoListOfSurveyListEntries(Survey survey) {
        final List<Leg> chronoLegs = survey.getAllLegsInChronoOrder();
        List<SurveyListEntry> entries = toListOfSurveyListEntries(survey);
        Collections.sort(
                entries, (e0, e1) -> chronoLegs.indexOf(e0.leg) - chronoLegs.indexOf(e1.leg));
        return entries;
    }

    private List<SurveyListEntry> createListOfEntriesFromStation(Station from) {

        List<SurveyListEntry> list = new ArrayList<>();

        for (Leg leg : from.getUnconnectedOnwardLegs()) {
            SurveyListEntry entry = new SurveyListEntry(from, leg);
            list.add(entry);
        }

        for (Leg leg : from.getConnectedOnwardLegs()) {
            SurveyListEntry entry = new SurveyListEntry(from, leg);
            list.add(entry);
            list.addAll(createListOfEntriesFromStation(leg.getDestination()));
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
