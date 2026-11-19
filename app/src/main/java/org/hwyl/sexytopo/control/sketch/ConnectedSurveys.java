package org.hwyl.sexytopo.control.sketch;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

/**
 * The surveys linked to the one being shown, each moved so its linking station lines up with the
 * station it links to. The static method works them out; an instance keeps the result for a view,
 * working it out again only when the set of linked surveys changes.
 */
public class ConnectedSurveys {

    private Map<Survey, Space<Coord2D>> surveysToProjections = new HashMap<>();

    // the surveys directly linked to the one being shown when surveysToProjections was worked out;
    // the map also holds surveys linked through those, so it can't be compared with directly
    private Set<Survey> directlyLinkedSurveys = Collections.emptySet();

    /**
     * The linked surveys, lined up with the survey being shown, and where each is drawn. They're
     * worked out again only when the surveys directly linked to it change. If they can't be worked
     * out, the problem is logged and none are returned.
     */
    Map<Survey, Space<Coord2D>> getSurveysToProjections(
            Projection2D projectionType, Survey survey, Space<Coord2D> projection) {
        Set<Survey> linkedSurveys = getFlatSetOfConnectedSurveys(survey);
        if (!linkedSurveys.equals(directlyLinkedSurveys)) {
            try {
                surveysToProjections =
                        getTranslatedConnectedSurveys(projectionType, survey, projection);
                directlyLinkedSurveys = linkedSurveys;
            } catch (Exception exception) {
                Log.e("Error getting translated connected surveys");
                Log.e(exception);
                return Collections.emptyMap();
            }
        }
        return surveysToProjections;
    }

    /** Stop showing a linked survey, e.g. because it couldn't be drawn. */
    void forget(Survey survey) {
        surveysToProjections.remove(survey);
    }

    private static Set<Survey> getFlatSetOfConnectedSurveys(Survey survey) {
        Set<Survey> flatSet = new HashSet<>();
        for (Set<SurveyConnection> connectionSet : survey.getConnectedSurveys().values()) {
            for (SurveyConnection connection : connectionSet) {
                flatSet.add(connection.otherSurvey);
            }
        }
        return flatSet;
    }

    public static Map<Survey, Space<Coord2D>> getTranslatedConnectedSurveys(
            Projection2D projectionType, Survey survey, Space<Coord2D> projection) {
        Map<Survey, Space<Coord2D>> translated = new HashMap<>();
        updateTranslatedConnectedSurveys(projectionType, survey, translated, survey, projection);
        return translated;
    }

    private static void updateTranslatedConnectedSurveys(
            Projection2D projectionType,
            Survey original,
            Map<Survey, Space<Coord2D>> translated,
            Survey survey,
            Space<Coord2D> projection) {

        Map<Station, Set<SurveyConnection>> connections = survey.getConnectedSurveys();
        for (Station connectingStation : connections.keySet()) {

            Coord2D connectingStationLocation = projection.getStationMap().get(connectingStation);

            for (SurveyConnection connection : connections.get(connectingStation)) {

                Station otherConnectingStation = connection.stationInOtherSurvey;

                Survey otherSurvey = connection.otherSurvey;

                if (haveWeAlreadyDoneThisSurvey(translated, otherSurvey, original)) {
                    continue;
                }

                // create a new copy of the survey so we can edit the associated sketch
                // (this might seem a bit messy but it's reasonably elegant, honest!)
                Survey lightweightSurveyCopy = new Survey();
                lightweightSurveyCopy.setDirectory(otherSurvey.getDirectory());
                lightweightSurveyCopy.setOrigin(otherSurvey.getOrigin());

                Space<Coord2D> otherProjection = projectionType.project(connection.otherSurvey);
                Coord2D otherConnectingStationLocation =
                        otherProjection.getStationMap().get(otherConnectingStation);
                Coord2D transformation =
                        connectingStationLocation.minus(otherConnectingStationLocation);

                Sketch translatedPlan = otherSurvey.getPlanSketch().translate(transformation);
                lightweightSurveyCopy.setPlanSketch(translatedPlan);
                Sketch translatedElevation =
                        otherSurvey.getElevationSketch().translate(transformation);
                lightweightSurveyCopy.setElevationSketch(translatedElevation);

                otherProjection = Space2DUtils.translate(otherProjection, transformation);

                translated.put(lightweightSurveyCopy, otherProjection);

                updateTranslatedConnectedSurveys(
                        projectionType, original, translated, otherSurvey, otherProjection);
            }
        }
    }

    private static boolean haveWeAlreadyDoneThisSurvey(
            Map<Survey, Space<Coord2D>> translated, Survey survey, Survey original) {

        if (original.equals(survey)) {
            return true;
        }

        for (Survey doneSurvey : translated.keySet()) {
            if (doneSurvey.getUri().equals(survey.getUri())) {
                return true;
            }
        }
        return false;
    }
}
