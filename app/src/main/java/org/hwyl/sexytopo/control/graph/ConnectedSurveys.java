package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ConnectedSurveys {

    public static Map<Survey, Space<Coord2D>> getTranslatedConnectedSurveys(
            Projection2D projectionType, Survey survey, Space<Coord2D> projection) {
        Map<Survey, Space<Coord2D>> translated = new HashMap<>();
        updateTranslatedConnectedSurveys(projectionType, survey, translated, survey, projection);
        return translated;
    }


    private static void updateTranslatedConnectedSurveys(
            Projection2D projectionType, Survey original,
            Map<Survey, Space<Coord2D>> translated, Survey survey, Space<Coord2D> projection) {

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

                Sketch translatedPlan =
                        otherSurvey.getPlanSketch().getTranslatedCopy(transformation);
                lightweightSurveyCopy.setPlanSketch(translatedPlan);
                Sketch translatedElevation =
                        otherSurvey.getElevationSketch().getTranslatedCopy(transformation);
                lightweightSurveyCopy.setElevationSketch(translatedElevation);

                otherProjection = Space2DUtils.transform(otherProjection, transformation);

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
