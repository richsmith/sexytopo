package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SurveyJsonTranslater {

    public static final String SURVEY_NAME_TAG = "name";

    public static final String STATIONS_TAG = "stations";
    public static final String STATION_NAME_TAG = "name";
    public static final String DIRECTION_TAG = "eeDirection";
    public static final String COMMENT_TAG = "comment";

    public static final String ONWARD_LEGS_TAG = "legs";
    public static final String DISTANCE_TAG = "distance";
    public static final String AZIMUTH_TAG = "azimuth";
    public static final String INCLINATION_TAG = "inclination";
    public static final String PROMOTED_FROM_TAG = "promotedFrom";
    public static final String DESTINATION_TAG = "destination";
    public static final String WAS_SHOT_BACKWARDS_TAG = "wasShotBackwards";

    public static final String ACTIVE_STATION_TAG = "activeStation";


    public static String toText(Survey survey) throws JSONException {
        return toJson(survey).toString();
    }


    public static void populateSurvey(Survey survey, String string) throws JSONException {
        JSONObject json = new JSONObject(string);
        toSurvey(survey, json);
    }


    public static JSONObject toJson(Survey survey) throws JSONException {

        JSONObject json = new JSONObject();

        json.put(SURVEY_NAME_TAG, survey.getName());

        JSONArray stationArray = new JSONArray();
        for (Station station : survey.getAllStations()) {
            stationArray.put(toJson(station));
        }
        json.put(STATIONS_TAG, stationArray);

        return json;
    }

    public static void toSurvey(Survey survey, JSONObject json) throws JSONException {


        String name = json.getString(SURVEY_NAME_TAG);
        if (!survey.getName().equals(name)) {
            Log.e("This is weird; the survey name in the file is different to the filename. " +
                "Assuming filename is the correct name.");
        }

        try {
            JSONArray stationsArray = json.getJSONArray(STATIONS_TAG);
            Station origin = toTree(stationsArray);
            survey.setOrigin(origin);
        } catch (JSONException e) {
            Log.e("Failed to load stations: " + e);
        }

        try {
            String activeStationName = json.getString(ACTIVE_STATION_TAG);
            Station activeStation = survey.getStationByName(activeStationName);
            survey.setActiveStation(activeStation);
        } catch (Exception ignore) {
            // ah, never mind... not mission-critical
        }
    }


    public static JSONObject toJson(Station station) throws JSONException {

        JSONObject json = new JSONObject();
        json.put(STATION_NAME_TAG, station.getName());
        json.put(DIRECTION_TAG, station.getExtendedElevationDirection().toString().toLowerCase());
        json.put(COMMENT_TAG, station.getComment());

        JSONArray onwardLegsArray = new JSONArray();
        for (Leg leg : station.getOnwardLegs()) {
            onwardLegsArray.put(toJson(leg));
        }
        json.put(ONWARD_LEGS_TAG, onwardLegsArray);

        return json;
    }


    public static Station toTree(JSONArray json) throws JSONException {

        Map<String, Station> namesToStations = new HashMap<>();

        Station latest = null;
        List<JSONObject> stationData = Util.toList(json);
        Collections.reverse(stationData);
        for (JSONObject object : stationData) {
            Station station = toStation(namesToStations, object);
            namesToStations.put(station.getName(), station);
            latest = station;
        }

        return latest; // this *ought* to be the first in the file, i.e. the origin
    }


    public static JSONObject toJson(Leg leg) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(DISTANCE_TAG, leg.getDistance());
        json.put(AZIMUTH_TAG, leg.getAzimuth());
        json.put(INCLINATION_TAG, leg.getInclination());
        json.put(DESTINATION_TAG, leg.getDestination().getName());
        json.put(WAS_SHOT_BACKWARDS_TAG, leg.wasShotBackwards());

        JSONArray promotedFromArray = new JSONArray();
        for (Leg promotedLeg : leg.getPromotedFrom()) {
            promotedFromArray.put(toJson(promotedLeg));
        }
        json.put(PROMOTED_FROM_TAG, promotedFromArray);

        return json;
    }


    public static Station toStation(Map<String, Station> namesToStations,
                                    JSONObject json) throws JSONException {

        String name = json.getString(STATION_NAME_TAG);
        Station station = new Station(name);

        try {
            String comment = json.getString(COMMENT_TAG);
            station.setComment(comment);
        } catch (Exception ignore) {
            // not ideal but not the end of the world; we'd probably prefer to have our data
        }
        try {
            Direction direction = Direction.valueOf(json.getString(DIRECTION_TAG).toUpperCase());
            station.setExtendedElevationDirection(direction);
        } catch (Exception ignore) {
            // not ideal but not the end of the world; we'd probably prefer to have our data
        }

        JSONArray array = json.getJSONArray(ONWARD_LEGS_TAG);
        for (JSONObject object : Util.toList(array)) {
            Leg leg = toLeg(namesToStations, object);
            station.addOnwardLeg(leg);
        }

        return station;
    }


    public static Leg toLeg(Map<String, Station> namesToStations,
                            JSONObject json) throws JSONException {

        double distance = json.getDouble(DISTANCE_TAG);
        double azimuth = json.getDouble(AZIMUTH_TAG);
        double inclination = json.getDouble(INCLINATION_TAG);
        boolean wasShotBackwards = json.getBoolean(WAS_SHOT_BACKWARDS_TAG);

        String destinationName = json.getString(DESTINATION_TAG);

        Leg leg = null;
        if (destinationName.equals(SexyTopo.BLANK_STATION_NAME)) {
            leg = new Leg(distance, azimuth, inclination, wasShotBackwards);

        } else {
            if (!namesToStations.containsKey(destinationName)) {
                throw new JSONException(
                        "Survey file corrupted: station " + destinationName + " missing or out of order");
            }

            List<Leg> promotedFromList = new ArrayList<>();
            try {
                JSONArray array = json.getJSONArray(PROMOTED_FROM_TAG);
                for (JSONObject object : Util.toList(array)) {
                    Leg promotedFrom = toLeg(namesToStations, object);
                    promotedFromList.add(promotedFrom);
                }
            } catch (Exception ignore) {
                // not ideal but not the end of the world; we'd probably prefer to have our data
            }
            Leg[] promotedFrom = promotedFromList.toArray(new Leg[]{});

            Station destination = namesToStations.get(destinationName);
            leg = new Leg(distance, azimuth, inclination,
                    destination, promotedFrom, wasShotBackwards);
        }

        return leg;
    }


}
