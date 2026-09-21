package org.hwyl.sexytopo.testutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

public class BasicTestSurveyCreator {

    public static Survey createEmptySurvey() {
        return new Survey();
    }

    public static Survey createStraightNorthThroughRepeats() {
        Survey survey = new Survey();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Leg leg = new Leg(5, 0, 0);
                SurveyUpdater.update(survey, leg);
            }
        }
        return survey;
    }

    public static Survey createStraightNorth() {
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;
    }

    public static Survey createStraightSouth() {
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;
    }

    public static Survey createRightRight() {
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;
    }

    public static Survey create5MDown() {
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 0, -90);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        return survey;
    }

    public static Survey create5MEast() {
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg splay0Left = new Leg(1, 0, 0);
        SurveyUpdater.update(survey, splay0Left);
        Leg splay0Right = new Leg(1, 180, 0);
        SurveyUpdater.update(survey, splay0Right);

        return survey;
    }

    public static Survey createStraightNorthWith1EBranch() {
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        survey.setActiveStation(survey.getStationByName("1"));
        Leg legBranch = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch);

        return survey;
    }

    public static Survey createStraightNorthWith2EBranch() {
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        survey.setActiveStation(survey.getStationByName("1"));
        Leg legBranch = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch);

        Leg legBranch2 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch2);

        return survey;
    }

    public static Survey createStraightNorthWith2EBranchFromS2() {
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        survey.setActiveStation(survey.getStationByName("2"));
        Leg legBranch = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch);

        Leg legBranch2 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch2);

        return survey;
    }

    public static Survey createSpanningZeroBoundary() {
        // Creates a survey where station 2 has incoming leg at 350° and outgoing at 10°
        // This tests the 0/360 boundary handling for cross-section angle calculation
        Survey survey = new Survey();

        Leg leg0 = new Leg(5, 350, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 10, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        return survey;
    }

    /**
     * Branching survey with a couple of cross-sections attached to the plan sketch. Useful for
     * exercising cross-section export.
     */
    public static Survey createWithCrossSections() {
        Survey survey = createStraightNorthWith1EBranch();
        Sketch plan = survey.getSketch(Projection2D.PLAN);

        Station station1 = survey.getStationByName("1");
        CrossSection xs1 = CrossSectioner.section(survey, station1);
        plan.addCrossSection(xs1, new Coord2D(2, 0));

        Station station3 = survey.getStationByName("3");
        CrossSection xs3 = CrossSectioner.section(survey, station3);
        plan.addCrossSection(xs3, new Coord2D(2, 10));

        return survey;
    }

    /**
     * Like createWithCrossSections(), but with cross-sections on the elevation as well as the plan,
     * and with splays at the stations so that the cross-sections have something to show.
     *
     * <p>The plan has vertical cross-sections at stations 1 and 3. The elevation has a vertical one
     * at station 1 and a horizontal one at station 3, so each of those stations has a cross-section
     * in both sketches, and at station 3 they are of different kinds.
     *
     * <p>The splays at each of those stations are 2m east, 1.5m west, 1m north, 1m up and 1m down.
     * Facing north, a vertical section shows the east and west splays to the right and left, and
     * the up and down ones above and below, while the north one points straight at the viewer. A
     * horizontal section shows the east, west and north splays, and the up and down ones collapse
     * to nothing.
     */
    public static Survey createWithCrossSectionsInPlanAndElevation() {
        Survey survey = createStraightNorthWith1EBranch();
        Station station1 = survey.getStationByName("1");
        Station station3 = survey.getStationByName("3");

        // Splays first, as a cross-section's bounding box is worked out when it is created
        for (Station station : new Station[] {station1, station3}) {
            station.addOnwardLeg(new Leg(2, 90, 0));
            station.addOnwardLeg(new Leg(1.5f, 270, 0));
            station.addOnwardLeg(new Leg(1, 0, 0));
            station.addOnwardLeg(new Leg(1, 0, 90));
            station.addOnwardLeg(new Leg(1, 0, -90));
        }

        Sketch plan = survey.getSketch(Projection2D.PLAN);
        plan.addCrossSection(CrossSectioner.section(survey, station1), new Coord2D(2, 0));
        plan.addCrossSection(CrossSectioner.section(survey, station3), new Coord2D(2, 10));

        Sketch elevation = survey.getSketch(Projection2D.EXTENDED_ELEVATION);
        elevation.addCrossSection(
                CrossSectioner.section(survey, station1, CrossSection.Orientation.VERTICAL),
                new Coord2D(2, 4));
        elevation.addCrossSection(
                CrossSectioner.section(survey, station3, CrossSection.Orientation.HORIZONTAL),
                new Coord2D(12, 4));

        return survey;
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public static Survey createStraightNorthWithTrip() {
        Survey survey = new Survey();

        List<Trip.TeamEntry> team = new ArrayList<>();
        team.add(new Trip.TeamEntry("Alice", Arrays.asList(Trip.Role.BOOK)));
        team.add(new Trip.TeamEntry("Bob", Arrays.asList(Trip.Role.INSTRUMENTS, Trip.Role.DOG)));
        Trip trip = new Trip();
        trip.setTeam(team);
        survey.setTrip(trip);

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;
    }
}
