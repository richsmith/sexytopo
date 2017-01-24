package org.hwyl.sexytopo.model.survey;

public final class SurveyConnection {

    public final Station stationInOtherSurvey;
    public final Survey otherSurvey;

    public SurveyConnection(Station stationInOtherSurvey, Survey otherSurvey) {
        this.stationInOtherSurvey = stationInOtherSurvey;
        this.otherSurvey = otherSurvey;
    }
}
