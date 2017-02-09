package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MetadataTranslater {

    public static final String CONNECTIONS_TAG = "connections";
    public static final String ACTIVE_STATION_TAG = "active-station";

    public static String translate(Survey survey) throws Exception {
        return toJson(survey).toString();
    }


    public static void translateAndUpdate(Survey survey, String string) throws Exception {
        Set<String> surveyNamesNotToLoad = new HashSet<>();
        translateAndUpdate(survey, string, surveyNamesNotToLoad);
    }


    public static void translateAndUpdate(
            Survey survey, String string, Set<String> surveyNamesNotToLoad) throws Exception {
        JSONObject json = new JSONObject(string);
        translateAndUpdate(survey, json, surveyNamesNotToLoad);
    }


    public static JSONObject toJson(Survey survey) throws JSONException {

        JSONObject json = new JSONObject();

        String activeStationName = survey.getActiveStation().getName();
        json.put(ACTIVE_STATION_TAG, activeStationName);


        Map<Station, Set<SurveyConnection>> connectedSurveys = survey.getConnectedSurveys();
        JSONObject jsonMap = toJson(connectedSurveys);
        json.put(CONNECTIONS_TAG, jsonMap);

        return json;
    }

    private static JSONObject toJson(Map<Station, Set<SurveyConnection>> connectedSurveys)
            throws JSONException {

        JSONObject connectionMap = new JSONObject();

        for (Station connectionPoint : connectedSurveys.keySet()) {
            Set<SurveyConnection> connections = connectedSurveys.get(connectionPoint);
            JSONArray setObject = new JSONArray();
            for (SurveyConnection connection : connections) {
                JSONArray connectionObject = toJson(connection);
                setObject.put(connectionObject);
            }
            connectionMap.put(connectionPoint.getName(), setObject);
        }

        return connectionMap;
    }

    private static JSONArray toJson(SurveyConnection connection) throws JSONException {
        JSONArray pair = new JSONArray();
        pair.put(connection.otherSurvey.getName());
        pair.put(connection.stationInOtherSurvey.getName());
        return pair;
    }


    private static void translateAndUpdate(Survey survey, JSONObject json, Set<String> surveyNamesNotToLoad) throws Exception {
        translateAndUpdateActiveStation(survey, json);
        translateAndUpdateConnections(survey, json, surveyNamesNotToLoad);
    }

    private static void translateAndUpdateActiveStation(Survey survey, JSONObject json)
            throws Exception {
        try {
            String activeStationName = json.getString(ACTIVE_STATION_TAG);
            Station activeStation = survey.getStationByName(activeStationName);
            survey.setActiveStation(activeStation);
        } catch (JSONException exception) {
            Log.e("Could not load active station: " + exception.toString());
            throw new Exception("Error loading active station");
        }
    }


    private static void translateAndUpdateConnections(
            Survey survey, JSONObject json, Set<String> surveyNamesNotToLoad) throws Exception {

       try {
           JSONObject connectionsObject = json.getJSONObject(CONNECTIONS_TAG);
           Map<String, JSONArray> outerMap = toMap(connectionsObject);

           for (String stationName : outerMap.keySet()) {

               Station station = survey.getStationByName(stationName);

               JSONArray setArray = outerMap.get(stationName);

               for (JSONArray connectionPair : toList(setArray)) {

                   assert connectionPair.length() == 2;
                   String connectedSurveyName = connectionPair.get(0).toString();
                   String connectionPointName = connectionPair.get(1).toString();

                   if (surveyNamesNotToLoad.contains(connectedSurveyName)) {
                       // if this survey is already loaded, don't do the whole infinite loop thing
                       continue;

                   } else {
                       // we *really* want to avoid infinite loops :)
                       // this should be added on load, but just as a precaution..
                       surveyNamesNotToLoad.add(connectedSurveyName);

                       try {
                           Survey connectedSurvey =
                                   Loader.loadSurvey(connectedSurveyName, surveyNamesNotToLoad);
                           Station connectionPoint =
                                   connectedSurvey.getStationByName(connectionPointName);
                           if (connectionPoint == null) {
                               Log.e("Connection point not found: " + connectionPoint.getName());
                               throw new Exception("Connection point not found");
                           }
                           survey.connect(station, connectedSurvey, connectionPoint);
                       } catch (Exception exception) {
                           // the linked survey or connecting station has probably been deleted;
                           // not much we can do...
                           Log.e("Could not load connected survey " + connectedSurveyName);
                           continue;
                       }
                   }
               }
           }

       } catch (JSONException exception) {
           Log.e(exception.toString());
           throw new Exception("Error loading connected surveys");
       }
    }


    private static List<JSONArray> toList(JSONArray array) throws JSONException {
        List<JSONArray> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getJSONArray(i));
        }
        return list;
    }


    private static Map<String, JSONArray> toMap(JSONObject object) throws JSONException {
        Map<String, JSONArray> map = new HashMap<>();
        Iterator iterator = object.keys();
        while (iterator.hasNext()) {
            String key = (String)iterator.next();
            JSONArray value = object.getJSONArray(key);
            map.put(key, value);
        }
        return map;
    }
}
