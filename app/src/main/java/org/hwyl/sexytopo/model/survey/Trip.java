package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Trip {

    public enum Role {
        BOOK(R.string.trip_role_book),
        INSTRUMENTS(R.string.trip_role_instruments),
        DOG(R.string.trip_role_dog),
        EXPLORATION(R.string.trip_role_exploration),;

        public final int descriptionId;
        Role(int descriptionId) {
            this.descriptionId = descriptionId;
        }
    }

    public static class TeamEntry {
        public final String name;
        public final List<Role> roles;
        public TeamEntry(String name, List<Role> roles) {
            this.name = name;
            this.roles = roles;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof TeamEntry)) {
                return false;
            }
            TeamEntry otherEntry = (TeamEntry)other;
            if (! otherEntry.name.equals(name)) {
                return false;
            }

            if (otherEntry.roles.size() != roles.size()) {
                return false;
            }

            for (int i = 0; i < roles.size(); i++) {
                if (otherEntry.roles.get(i) != roles.get(i)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + name.hashCode();
            for (Role role : roles) {
                result = 31 * result + role.hashCode();
            }
            return result;
        }
        
        // Helper to check if this team entry has any roles
        public boolean hasRoles() {
            return roles != null && !roles.isEmpty();
        }
    }

    private Date date;
    private List<TeamEntry> team = new ArrayList<>();
    private String comments;
    private String instrument;  // Instrument name/description
    private Date explorationDate;  // Exploration date field
    private boolean explorationDateSameAsSurvey = true;  // Default to same as survey


    public Trip() {
        this.date = new Date();
        this.instrument = "";  // Default to empty string
        this.comments = "";  // Default to empty string
    }

    public List<TeamEntry> getTeam() {
        return team;
    }


    public void setTeam(List<TeamEntry> team) {
        this.team = team;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    // Instrument methods
    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public boolean hasInstrument() {
        return instrument != null && !instrument.trim().isEmpty();
    }

    // Exploration date methods
    public Date getExplorationDate() {
        return explorationDate;
    }

    public void setExplorationDate(Date explorationDate) {
        this.explorationDate = explorationDate;
    }

    public boolean isExplorationDateSameAsSurvey() {
        return explorationDateSameAsSurvey;
    }

    public void setExplorationDateSameAsSurvey(boolean sameAsSurvey) {
        this.explorationDateSameAsSurvey = sameAsSurvey;
    }

    public boolean equalsTripData(Trip trip) {
        // Note, not "equals" method because the trips could have same data but not the same ID

        if (objectsNotEqual(trip.date, date)) {
            return false;
        }

        if (objectsNotEqual(trip.comments, comments)) {
            return false;
        }

        if (objectsNotEqual(trip.instrument, instrument)) {
            return false;
        }

        if (objectsNotEqual(trip.explorationDate, explorationDate)) {
            return false;
        }

        if (trip.explorationDateSameAsSurvey != explorationDateSameAsSurvey) {
            return false;
        }

        if (trip.team.size() != team.size()) {
            return false;
        }

        for (int i = 0; i < team.size(); i++) {
            TeamEntry otherEntry = trip.team.get(i);
            TeamEntry thisEntry = team.get(i);
            if (! thisEntry.equals(otherEntry)) {
                return false;
            }
        }

        return true;
    }

    private static boolean objectsNotEqual(Object a, Object b) {
        if (a == null) return b != null;
        return !a.equals(b);
    }

}
