package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SurveyJsonTranslater {

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final String VERSION_NAME_TAG = "sexyTopoVersionName";
    public static final String VERSION_CODE_TAG = "sexyTopoVersionCode";

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

    public static final String TRIP_TAG = "trip";
    public static final String TRIP_DATE_TAG = "tripDate";
    public static final String TEAM_TAG = "team";
    public static final String TEAM_MEMBER_NAME_TAG = "name";
    public static final String TEAM_MEMBER_ROLE_TAG = "role";

    public static String toText(Survey survey, String versionName, int versionCode)
            throws JSONException {
        return toJson(survey, versionName, versionCode).toString();
    }


    public static void populateSurvey(Survey survey, String string)
            throws JSONException, ParseException {
        JSONObject json = new JSONObject(string);
        toSurvey(survey, json);
    }


    public static JSONObject toJson(Survey survey, String versionName, int versionCode)
            throws JSONException {

        JSONObject json = new JSONObject();
        json.put(VERSION_NAME_TAG, versionName);
        json.put(VERSION_CODE_TAG, versionCode);
        json.put(SURVEY_NAME_TAG, survey.getName());

        JSONArray stationArray = new JSONArray();
        for (Station station : survey.getAllStations()) {
            stationArray.put(toJson(station));
        }
        json.put(STATIONS_TAG, stationArray);

        if (survey.getTrip() != null) {
            JSONObject trip = toJson(survey.getTrip());
            json.put(TRIP_TAG, trip);
        }

        return json;
    }


    public static void toSurvey(Survey survey, JSONObject json)
            throws JSONException, ParseException {

        String name = json.getString(SURVEY_NAME_TAG);
        if (!survey.getName().equals(name)) {
            Log.e("This is weird; the survey name in the file is different to the filename. " +
                "Assuming filename is the correct name.");
        }

        try { // have to parse trips before stations etc. so trips can be referenced by them
            if (json.has(TRIP_TAG)) {
                JSONObject tripObject = json.getJSONObject(TRIP_TAG);
                Trip trip = toTrip(tripObject);
                survey.setTrip(trip);
            }
        } catch (JSONException exception) {
            Log.e("Failed to load trip: " + exception);
            // carry on... unfortunate, but not *that* important
        }

        try {
            JSONArray stationsArray = json.getJSONArray(STATIONS_TAG);
            Station origin = toTree(stationsArray);
            survey.setOrigin(origin);
        } catch (JSONException exception) {
            Log.e("Failed to load stations: " + exception);
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


    public static JSONObject toJson(Trip trip) throws JSONException {

        JSONObject json = new JSONObject();

        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        String date = dateFormat.format(trip.getDate());
        json.put(TRIP_DATE_TAG, date);
        json.put(COMMENT_TAG, trip.getComments());

        JSONArray teamArray = new JSONArray();

        for (Trip.TeamEntry teamEntry: trip.getTeam()) {
            JSONObject teamEntryJson = new JSONObject();
            teamEntryJson.put(TEAM_MEMBER_NAME_TAG, teamEntry.name);
            JSONArray rolesJson = new JSONArray();
            for (Trip.Role role : teamEntry.roles) {
                rolesJson.put(role.name());
            }
            teamEntryJson.put(TEAM_MEMBER_ROLE_TAG, rolesJson);
            teamArray.put(teamEntryJson);
        }

        json.put(TEAM_TAG, teamArray);
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


    public static Leg toLeg(
            Map<String, Station> namesToStations,
            JSONObject json) throws JSONException {

        double distance = json.getDouble(DISTANCE_TAG);
        double azimuth = json.getDouble(AZIMUTH_TAG);
        double inclination = json.getDouble(INCLINATION_TAG);
        boolean wasShotBackwards = json.getBoolean(WAS_SHOT_BACKWARDS_TAG);

        String destinationName = json.getString(DESTINATION_TAG);

        Leg leg;
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


    public static Trip toTrip(JSONObject json) throws JSONException, ParseException {

        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        String dateString = json.getString(TRIP_DATE_TAG);
        Date date = dateFormat.parse(dateString);
        json.put(TRIP_DATE_TAG, date);

        String comments = json.getString(COMMENT_TAG);

        JSONArray teamArray = json.getJSONArray(TEAM_TAG);
        List<Trip.TeamEntry> team = new ArrayList<>();
        for (JSONObject teamEntryJson : Util.toList(teamArray)) {
            String name = teamEntryJson.getString(TEAM_MEMBER_NAME_TAG);
            JSONArray rolesArray = teamEntryJson.getJSONArray(TEAM_MEMBER_ROLE_TAG);
            List<Trip.Role> roles = new ArrayList<>();
            for (String roleString : Util.toListOfStrings(rolesArray)) {
                Trip.Role role = Trip.Role.valueOf(roleString);
                roles.add(role);
            }
            Trip.TeamEntry teamEntry = new Trip.TeamEntry(name, roles);
            team.add(teamEntry);
        }

        Trip trip = new Trip();
        trip.setDate(date);
        trip.setTeam(team);
        trip.setComments(comments);
        return trip;
    }

}
