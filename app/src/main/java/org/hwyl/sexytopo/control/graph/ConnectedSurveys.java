package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.control.activity.GraphActivity;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
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
            GraphActivity activity, Survey survey, Space<Coord2D> projection) {
        Map<Survey, Space<Coord2D>> translated = new HashMap<>();
        updateTranslatedConnectedSurveys(activity, survey, translated, survey, projection);
        return translated;
    }


    private static void updateTranslatedConnectedSurveys(
            GraphActivity activity, Survey original,
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

                Space<Coord2D> otherProjection = activity.getProjection(connection.otherSurvey);
                Coord2D otherConnectingStationLocation = otherProjection.getStationMap().get(otherConnectingStation);
                Coord2D transformation = connectingStationLocation.minus(otherConnectingStationLocation);

                Sketch sketch = activity.getSketch(otherSurvey);
                sketch.updateWithTranslation(transformation);

                otherProjection = Space2DUtils.transform(otherProjection, transformation);

                translated.put(otherSurvey, otherProjection);

                updateTranslatedConnectedSurveys(activity, original, translated, otherSurvey, otherProjection);
            }

        }
    }

    private static boolean haveWeAlreadyDoneThisSurvey(Map<Survey, Space<Coord2D>> translated, Survey survey, Survey original) {

        if (original.getName().equals(survey.getName())) {
            return true;
        }

        for (Survey doneSurvey : translated.keySet()) {
            if (doneSurvey.getName().equals(survey.getName())) {
                return true;
            }
        }
        return false;
    }

}
