package org.hwyl.sexytopo.control.util;

import static org.hwyl.sexytopo.control.util.StationNameRule.Action.CREATE;
import static org.hwyl.sexytopo.control.util.StationNameRule.Action.MOVE;
import static org.hwyl.sexytopo.control.util.StationNameRule.Action.NONE;
import static org.hwyl.sexytopo.control.util.StationNameRule.Action.RENAME;
import static org.hwyl.sexytopo.control.util.StationNameRule.Error.ALREADY_EXISTS;
import static org.hwyl.sexytopo.control.util.StationNameRule.Error.BLANK;
import static org.hwyl.sexytopo.control.util.StationNameRule.Error.BREAKS_CONTINUITY;
import static org.hwyl.sexytopo.control.util.StationNameRule.Error.DOES_NOT_EXIST;
import static org.hwyl.sexytopo.control.util.StationNameRule.Error.RESERVED;

import org.hwyl.sexytopo.control.util.StationNameRule.Result;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

/** Survey used throughout: 1-2-3-4 with a branch 1-5. */
public class StationNameRuleTest {

    private final Survey survey = BasicTestSurveyCreator.createStraightNorthWith1EBranch();

    private Station station(String name) {
        return survey.getStationByName(name);
    }

    private Leg legTo(String name) {
        return survey.getReferringLeg(station(name));
    }

    /** Expected is an Action when valid, or an Error when refused. */
    private static void assertResult(String message, Object expected, Result result) {
        if (expected instanceof StationNameRule.Error) {
            Assert.assertEquals(message, expected, result.error);
        } else {
            Assert.assertNull(message, result.error);
            Assert.assertEquals(message, expected, result.action);
        }
    }

    @Test
    public void testCheckExisting() {
        // existing station, leg's new station (null when adding), typed name, expected
        Object[][] cases = {
            {"2", "3", "2", NONE},
            {"2", "3", "", BLANK},
            {"2", "3", "-", RESERVED},
            {"2", "3", ".", RESERVED},
            {"2", "3", "X", RENAME},
            {"2", "3", "1", MOVE},
            {"2", "3", "5", MOVE},
            {"2", "3", "3", BREAKS_CONTINUITY},
            {"2", "3", "4", BREAKS_CONTINUITY},
            {"1", "2", "X", RENAME},
            {"1", "2", "5", MOVE},
            {"1", "2", "3", BREAKS_CONTINUITY},
            {"4", null, "4", NONE},
            {"4", null, "1", MOVE},
            {"4", null, "X", RENAME},
        };
        for (Object[] c : cases) {
            String message = c[0] + "-" + c[1] + " typed " + c[2];
            Leg leg = c[1] == null ? null : legTo((String) c[1]);
            Result result =
                    StationNameRule.checkExisting(
                            survey, station((String) c[0]), leg, (String) c[2]);
            assertResult(message, c[3], result);
            if (result.action == MOVE) {
                Assert.assertSame(message, station((String) c[2]), result.target);
            }
        }
    }

    @Test
    public void testCheckSplayStation() {
        Object[][] cases = {
            {"4", NONE}, {"1", MOVE}, {"X", DOES_NOT_EXIST}, {"", BLANK}, {"-", RESERVED},
        };
        for (Object[] c : cases) {
            Result result = StationNameRule.checkSplayStation(survey, station("4"), (String) c[0]);
            assertResult("typed " + c[0], c[1], result);
        }
    }

    @Test
    public void testCheckNew() {
        Result renaming = StationNameRule.checkExisting(survey, station("2"), legTo("3"), "X");
        Result moving = StationNameRule.checkExisting(survey, station("2"), legTo("3"), "5");
        // new station (null when adding), existing box result, typed name, expected
        Object[][] cases = {
            {"3", null, "3", NONE},
            {"3", null, "X", RENAME},
            {"3", null, "2", ALREADY_EXISTS},
            {"3", null, "", BLANK},
            {"3", null, ".", RESERVED},
            {null, null, "X", CREATE},
            {null, null, "1", ALREADY_EXISTS},
            {"3", renaming, "X", ALREADY_EXISTS},
            {"3", moving, "X", RENAME},
        };
        for (Object[] c : cases) {
            String message = c[0] + " typed " + c[2];
            Station station = c[0] == null ? null : station((String) c[0]);
            Result result = StationNameRule.checkNew(survey, station, (String) c[2], (Result) c[1]);
            assertResult(message, c[3], result);
        }
    }

    @Test
    public void testApplyRenameRenamesEverywhere() {
        Station two = station("2");
        Result result = StationNameRule.checkExisting(survey, two, legTo("3"), "X");
        Station named = SurveyUpdater.applyStationName(survey, two, legTo("3"), result);
        Assert.assertSame(two, named);
        Assert.assertSame(two, station("X"));
        Assert.assertSame(two, legTo("X").getDestination());
        Assert.assertNull(station("2"));
    }

    @Test
    public void testApplyMoveTakesStationsBeyond() {
        Leg leg = legTo("3");
        Result result = StationNameRule.checkExisting(survey, station("2"), leg, "5");
        Station named = SurveyUpdater.applyStationName(survey, station("2"), leg, result);
        Assert.assertSame(station("5"), named);
        Assert.assertSame(station("5"), survey.getOriginatingStation(leg));
        Assert.assertSame(station("3"), survey.getOriginatingStation(legTo("4")));
        Assert.assertEquals(5, survey.getAllStations().size());
    }

    @Test
    public void testApplyMoveWhenAddingOnlyPicksStation() {
        Result result = StationNameRule.checkExisting(survey, station("4"), null, "1");
        Station named = SurveyUpdater.applyStationName(survey, station("4"), null, result);
        Assert.assertSame(station("1"), named);
        Assert.assertSame(station("3"), survey.getOriginatingStation(legTo("4")));
    }

    @Test
    public void testApplyMoveSplayMovesOnlyThatShot() {
        Leg splay = new Leg(1, 0, 0);
        Leg other = new Leg(2, 0, 0);
        station("4").addOnwardLeg(splay);
        station("4").addOnwardLeg(other);
        Result result = StationNameRule.checkSplayStation(survey, station("4"), "1");
        SurveyUpdater.applyStationName(survey, station("4"), splay, result);
        Assert.assertSame(station("1"), survey.getOriginatingStation(splay));
        Assert.assertSame(station("4"), survey.getOriginatingStation(other));
    }

    @Test
    public void testApplyCreateReturnsNewStation() {
        Result result = StationNameRule.checkNew(survey, null, "X", null);
        Station named = SurveyUpdater.applyStationName(survey, null, null, result);
        Assert.assertEquals("X", named.getName());
        Assert.assertNull(station("X"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplyRefusedNameFails() {
        Result result = StationNameRule.checkNew(survey, null, "1", null);
        SurveyUpdater.applyStationName(survey, null, null, result);
    }

    @Test
    public void testMoveLegBeyondItselfFailsAndLeavesSurvey() {
        Leg leg = legTo("3");
        try {
            SurveyUpdater.moveLeg(survey, leg, station("4"));
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            Assert.assertSame(station("2"), survey.getOriginatingStation(leg));
        }
    }
}
