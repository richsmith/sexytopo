package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GraphToListTranslator {


    public List<SurveyListEntry> toListOfSurveyListEntries(Survey survey) {
        return createListOfEntriesFromStation(survey.getOrigin());
    }


    public List<SurveyListEntry> toChronoListOfSurveyListEntries(Survey survey) {
        final List<Leg> chronoLegs = survey.getAllLegsInChronoOrder();
        List<SurveyListEntry> entries = toListOfSurveyListEntries(survey);
        Collections.sort(entries, (e0, e1) ->
                chronoLegs.indexOf(e0.leg) - chronoLegs.indexOf(e1.leg));
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

        Station from = entry.getFrom();
        Leg leg = entry.getLeg();

        Map<TableCol, Object> map = new HashMap<>();

        if (leg.wasShotBackwards()) {
            map.put(TableCol.FROM, leg.getDestination());
            map.put(TableCol.TO, from);
            leg = leg.asBacksight();
        } else {
            map.put(TableCol.FROM, from);
            map.put(TableCol.TO, leg.getDestination());
        }
        map.put(TableCol.DISTANCE, leg.getDistance());
        map.put(TableCol.AZIMUTH, leg.getAzimuth());
        map.put(TableCol.INCLINATION, leg.getInclination());

        return map;
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
