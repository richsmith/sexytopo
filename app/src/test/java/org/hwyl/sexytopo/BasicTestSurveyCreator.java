package org.hwyl.sexytopo;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Created by rls on 13/10/15.
 */
public class BasicTestSurveyCreator {

    public static Survey createStraightNorth() {
        Survey survey = new Survey("Test Straight Survey North");

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;
    }

    public static Survey createStraightSouth() {
        Survey survey = new Survey("Test Straight Survey North");

        Leg leg0 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;

    }

    public static Survey createRightRight() {
        Survey survey = new Survey("Test n-shaped Survey");

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;

    }
}
