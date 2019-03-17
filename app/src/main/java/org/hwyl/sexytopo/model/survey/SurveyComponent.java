package org.hwyl.sexytopo.model.survey;

/**
 * Superclass of components used to make up a survey. No functionality; just used
 * to refer to components generically.
 */
public abstract class SurveyComponent {

    private Trip trip;


    public Trip getTrip() {
        return trip;
    }


    public void setTrip(Trip trip) {
        this.trip = trip;
    }
}
