package org.hwyl.sexytopo.testhelpers;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;


public class BasicTestSurveyCreator {

    public static Survey createStraightNorthThroughRepeats() {
        Survey survey = new Survey("Test Straight Survey North");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Leg leg = new Leg(5, 0, 0);
                SurveyUpdater.update(survey, leg);
            }
        }
        return survey;
    }

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

    public static Survey create5MDown() {
        Survey survey = new Survey("Test Survey 1m Down");

        Leg leg0 = new Leg(5, 0, -90);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        return survey;
    }

    public static Survey create5MEast() {
        Survey survey = new Survey("Test Survey 5m east");

        Leg leg0 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg splay0Left = new Leg(1, 0, 0);
        SurveyUpdater.update(survey, splay0Left);
        Leg splay0Right = new Leg(1, 180, 0);
        SurveyUpdater.update(survey, splay0Right);

        return survey;
    }

}
