package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MetadataTranslater {

    public static final String CONNECTIONS_TAG = "connections";
    public static final String ACTIVE_STATION_TAG = "active-station";


    public static String translate(Survey survey) throws Exception {
        return toJson(survey).toString(SexyTopoConstants.JSON_INDENT);
    }


    public static void translateAndUpdate(Context context, Survey survey, String string)
            throws Exception {
        Set<Uri> surveyUrisNotToLoad = new HashSet<>();
        translateAndUpdate(context, survey, string, surveyUrisNotToLoad);
    }


    public static void translateAndUpdate(
            Context context,
            Survey survey,
            String string,
            Set<Uri> surveyUrisNotToLoad)
            throws Exception {
        JSONObject json = new JSONObject(string);
        translateAndUpdate(context, survey, json, surveyUrisNotToLoad);
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
            if (connections == null) {
                connections = new HashSet<>();
            }
            JSONArray setObject = new JSONArray();
            for (SurveyConnection connection : connections) {
                JSONArray connectionObject = toJson(connection);
                setObject.put(connectionObject);
            }
            connectionMap.put(connectionPoint.getName(), setObject);
        }

        return connectionMap;
    }


    private static JSONArray toJson(SurveyConnection connection) {
        JSONArray pair = new JSONArray();
        pair.put(connection.otherSurvey.getUri().toString());
        pair.put(connection.stationInOtherSurvey.getName());
        return pair;
    }


    private static void translateAndUpdate(
            Context context, Survey survey, JSONObject json, Set<Uri> surveyUrisNotToLoad)
            throws Exception {
        translateAndUpdateActiveStation(survey, json);
        translateAndUpdateConnections(context, survey, json, surveyUrisNotToLoad);
    }


    private static void translateAndUpdateActiveStation(Survey survey, JSONObject json)
            throws Exception {
        try {
            String activeStationName = json.getString(ACTIVE_STATION_TAG);
            Station activeStation = survey.getStationByName(activeStationName);
            survey.setActiveStation(activeStation);
        } catch (JSONException exception) {
            Log.e("Could not load active station: " + exception);
            throw new Exception("Error loading active station");
        }
    }


    @SuppressWarnings("UnnecessaryContinue")
    private static void translateAndUpdateConnections(
            Context context,
            Survey survey,
            JSONObject json,
            Set<Uri> surveyUrisNotToLoad)
            throws Exception {
        try {
            JSONObject connectionsObject = json.getJSONObject(CONNECTIONS_TAG);
            Map<String, JSONArray> outerMap = IoUtils.toMap(connectionsObject);

            for (String stationName : outerMap.keySet()) {

                Station station = survey.getStationByName(stationName);

                JSONArray setArray = outerMap.get(stationName);

                for (JSONArray connectionPair : toListOfArrays(setArray)) {

                    assert connectionPair.length() == 2;
                    String connectedSurveyUriString = connectionPair.get(0).toString();
                    Uri connectedSurveyUri = Uri.parse(connectedSurveyUriString);
                    String connectionPointName = connectionPair.get(1).toString();

                    if (surveyUrisNotToLoad.contains(connectedSurveyUri)) {
                        // if this survey is already loaded, don't do the whole infinite loop thing
                        continue;

                    } else {
                        // we *really* want to avoid infinite loops :)
                        // this should be added on load, but just as a precaution..
                        surveyUrisNotToLoad.add(connectedSurveyUri);

                        try {
                            DocumentFile directory =
                                    DocumentFile.fromTreeUri(context, connectedSurveyUri);
                            Survey connectedSurvey = Loader.loadSurvey(
                                    context, directory, surveyUrisNotToLoad, false);
                            Station connectionPoint =
                                    connectedSurvey.getStationByName(connectionPointName);
                            if (connectionPoint == null) {
                                Log.e("Connection point not found for survey "
                                        + connectedSurvey.getName());
                                throw new Exception("Connection point not found");
                            }
                            survey.connect(station, connectedSurvey, connectionPoint);
                        } catch (Exception exception) {
                            // the linked survey or connecting station has probably been deleted;
                            // not much we can do...
                            Log.e("Could not load connected survey " + connectedSurveyUri);
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

    public static List<JSONArray> toListOfArrays(JSONArray array) throws JSONException {
        List<JSONArray> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getJSONArray(i));
        }
        return list;
    }

}

