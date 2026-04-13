package org.hwyl.sexytopo.model.survey;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;

public class TripTest {

    private static Trip basicTrip() {
        Trip trip = new Trip();
        trip.setTeam(
                Arrays.asList(
                        new Trip.TeamEntry("Rich", Arrays.asList(Trip.Role.BOOK)),
                        new Trip.TeamEntry("Rufus", Arrays.asList(Trip.Role.INSTRUMENTS))));
        trip.setComments("Test comment");
        trip.setInstrument("DistoX");
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
        a.setSurveyDate(b.getSurveyDate());
        Assert.assertEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentComments() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setSurveyDate(b.getSurveyDate());
        b.setComments("Different");
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentInstrument() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setSurveyDate(b.getSurveyDate());
        b.setInstrument("SAP5");
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentTeamSize() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setSurveyDate(b.getSurveyDate());
        b.setTeam(
                Collections.singletonList(
                        new Trip.TeamEntry("Alice", Arrays.asList(Trip.Role.BOOK))));
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentTeamMemberName() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setSurveyDate(b.getSurveyDate());
        b.setTeam(
                Arrays.asList(
                        new Trip.TeamEntry("Alice", Arrays.asList(Trip.Role.BOOK)),
                        new Trip.TeamEntry("Bob", Arrays.asList(Trip.Role.INSTRUMENTS))));
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

    @Test
    public void testDefaultLinkedState() {
        Trip trip = new Trip();
        Assert.assertTrue(trip.isExplorationDateLinked());
        Assert.assertNull(trip.getExplorationDate());
        Assert.assertFalse(trip.hasExplorationDate());
    }

    @Test
    public void testUnlinkedWithNoExplorationDate() {
        Trip trip = new Trip();
        trip.setExplorationDateLinked(false);
        Assert.assertFalse(trip.isExplorationDateLinked());
        Assert.assertNull(trip.getExplorationDate());
        Assert.assertTrue(trip.hasExplorationDate());
    }

    @Test
    public void testUnlinkedWithExplorationDate() {
        Trip trip = new Trip();
        trip.setExplorationDateLinked(false);
        trip.setExplorationDate(new Date(0));
        Assert.assertFalse(trip.isExplorationDateLinked());
        Assert.assertNotNull(trip.getExplorationDate());
        Assert.assertTrue(trip.hasExplorationDate());
    }

    @Test
    public void testLinkedTripsEqual() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setSurveyDate(b.getSurveyDate());
        Assert.assertEquals(a, b);
    }

    @Test
    public void testNotEqualWhenLinkedDiffers() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setSurveyDate(b.getSurveyDate());
        b.setExplorationDateLinked(false);
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testToNextTripCopiesTeamAndInstrument() {
        Trip original = basicTrip();
        Trip next = original.toNextTrip();
        Assert.assertEquals(original.getTeam(), next.getTeam());
        Assert.assertEquals(original.getInstrument(), next.getInstrument());
    }

    @Test
    public void testToNextTripHasFreshDateAndEmptyComments() {
        Trip original = basicTrip();
        original.setSurveyDate(new Date(0));
        Trip next = original.toNextTrip();
        Assert.assertNotEquals(new Date(0), next.getSurveyDate());
        Assert.assertEquals("", next.getComments());
    }

    @Test
    public void testToNextTripTeamIsIndependent() {
        Trip original = basicTrip();
        Trip next = original.toNextTrip();
        next.setTeam(
                Collections.singletonList(
                        new Trip.TeamEntry("New", Arrays.asList(Trip.Role.BOOK))));
        Assert.assertEquals(2, original.getTeam().size());
    }

    @Test
    public void testNotEqualWhenExplorationDateDiffers() {
        Trip a = basicTrip();
        Trip b = basicTrip();
        a.setSurveyDate(b.getSurveyDate());
        a.setExplorationDateLinked(false);
        a.setExplorationDate(new Date(1000));
        b.setExplorationDateLinked(false);
        b.setExplorationDate(new Date(2000));
        Assert.assertNotEquals(a, b);
    }
}
