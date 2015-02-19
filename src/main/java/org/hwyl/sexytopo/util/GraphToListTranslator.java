package org.hwyl.sexytopo.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rls on 24/07/14.
 */
public class GraphToListTranslator {


    private static GraphToListTranslator instance = new GraphToListTranslator();

    public List<SurveyListEntry> toListOfSurveyListEntries(Survey survey) {
        return createListOfEntriesFromStation(survey.getOrigin());
    }



    public List<Map<TableCol, Object>> toListOfColMaps(Survey survey) {
        List<SurveyListEntry> surveyListEntries = toListOfSurveyListEntries(survey);
        List<Map<TableCol, Object>> newList = new ArrayList<>(surveyListEntries.size());
        for (GraphToListTranslator.SurveyListEntry entry : surveyListEntries) {
            Map<TableCol, Object> map = createMap(entry);
            newList.add(map);
        }
        return newList;
    }


    private List<SurveyListEntry> createListOfEntriesFromStation(Station from) {

        List<SurveyListEntry> list = new ArrayList<>();

        for (Leg leg : from.getOnwardLegs()) {

            SurveyListEntry entry = new SurveyListEntry(from, leg);
            list.add(entry);

            if (leg.hasDestination()) {
                list.addAll(createListOfEntriesFromStation(leg.getDestination()));
            }
        }

        return list;
    }


    public static Map<TableCol, Object> createMap(SurveyListEntry entry) {

        Station from = entry.getFrom();
        Leg leg = entry.getLeg();

        Map<TableCol, Object> map = new HashMap<>();

        map.put(TableCol.FROM, from);
        map.put(TableCol.TO, leg.getDestination());
        map.put(TableCol.DISTANCE, leg.getDistance());
        map.put(TableCol.BEARING, leg.getBearing());
        map.put(TableCol.INCLINATION, leg.getInclination());

        return map;
    }


    public class SurveyListEntry {
        private Station from;
        private Leg leg;

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
