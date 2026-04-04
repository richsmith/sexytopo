package org.hwyl.sexytopo.model.survey;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class TripTest {

    private static Trip basicTrip() {
        Trip trip = new Trip();
        trip.setTeam(Arrays.asList(
            new Trip.TeamEntry("Rich", Arrays.asList(Trip.Role.BOOK)),
            new Trip.TeamEntry("Rufus", Arrays.asList(Trip.Role.INSTRUMENTS))
        ));
        trip.setComments("Test comment");
        trip.setInstrument("DistoX");
        trip.setExplorationDateSameAsSurvey(true);
        return trip;
    }

    @Test
    public void testEqualsReflexive() {
        Trip trip = basicTrip();
        Assert.assertEquals(trip, trip);
    }

    @Test
    public void testEqualsIdenticalTrips() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setDate(b.getDate());
        Assert.assertEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentComments() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setDate(b.getDate());
        b.setComments("Different");
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentInstrument() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setDate(b.getDate());
        b.setInstrument("SAP5");
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentTeamSize() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setDate(b.getDate());
        b.setTeam(Collections.singletonList(
            new Trip.TeamEntry("Alice", Arrays.asList(Trip.Role.BOOK))
        ));
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentTeamMemberName() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setDate(b.getDate());
        b.setTeam(Arrays.asList(
            new Trip.TeamEntry("Alice", Arrays.asList(Trip.Role.BOOK)),
            new Trip.TeamEntry("Bob", Arrays.asList(Trip.Role.INSTRUMENTS))
        ));
        Assert.assertNotEquals(a, b);
    }


    @Test
    public void testCopyEqualsOriginal() {
        Trip original = basicTrip();
        Trip copy = new Trip(original);
        Assert.assertEquals(original, copy);
    }

    @Test
    public void testCopyIsIndependent() {
        Trip original = basicTrip();
        Trip copy = new Trip(original);
        copy.setComments("Modified");
        Assert.assertNotEquals(original, copy);
    }

}
