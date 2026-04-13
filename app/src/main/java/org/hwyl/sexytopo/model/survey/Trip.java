package org.hwyl.sexytopo.model.survey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hwyl.sexytopo.R;

public class Trip {

    public enum Role {
        BOOK(R.string.trip_role_book),
        INSTRUMENTS(R.string.trip_role_instruments),
        DOG(R.string.trip_role_dog),
        EXPLORATION(R.string.trip_role_exploration),
        ;

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
            TeamEntry otherEntry = (TeamEntry) other;
            if (!otherEntry.name.equals(name)) {
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

    private Date surveyDate;
    private Date explorationDate;
    private boolean explorationDateLinked;
    private List<TeamEntry> team = new ArrayList<>();
    private String comments;
    private String instrument;

    public Trip() {
        this.surveyDate = new Date();
        this.explorationDate = null;
        this.explorationDateLinked = true;
        this.instrument = "";
        this.comments = "";
    }

    public Trip(Trip other) {
        this.surveyDate = other.surveyDate;
        this.explorationDate = other.explorationDate;
        this.explorationDateLinked = other.explorationDateLinked;
        this.team = new ArrayList<>();
        for (TeamEntry entry : other.team) {
            this.team.add(new TeamEntry(entry.name, new ArrayList<>(entry.roles)));
        }
        this.comments = other.comments;
        this.instrument = other.instrument;
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

    public Date getSurveyDate() {
        return surveyDate;
    }

    public void setSurveyDate(Date surveyDate) {
        this.surveyDate = surveyDate;
    }

    public Date getExplorationDate() {
        return explorationDate;
    }

    public void setExplorationDate(Date explorationDate) {
        this.explorationDate = explorationDate;
    }

    public boolean isExplorationDateLinked() {
        return explorationDateLinked;
    }

    public void setExplorationDateLinked(boolean explorationDateLinked) {
        this.explorationDateLinked = explorationDateLinked;
    }

    public boolean hasExplorationDate() {
        return !explorationDateLinked || explorationDate != null;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public boolean hasInstrument() {
        return instrument != null && !instrument.trim().isEmpty();
    }

    /**
     * Creates a new Trip for a follow-on survey, copying team and instrument but with a fresh date
     * and empty comments.
     */
    public Trip toNextTrip() {
        Trip next = new Trip(this);
        next.setSurveyDate(new Date());
        next.setComments("");
        return next;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Trip)) {
            return false;
        }

        Trip trip = (Trip) other;

        if (objectsNotEqual(trip.surveyDate, surveyDate)) {
            return false;
        }

        if (objectsNotEqual(trip.explorationDate, explorationDate)) {
            return false;
        }

        if (trip.explorationDateLinked != explorationDateLinked) {
            return false;
        }

        if (objectsNotEqual(trip.comments, comments)) {
            return false;
        }

        if (objectsNotEqual(trip.instrument, instrument)) {
            return false;
        }

        if (trip.team.size() != team.size()) {
            return false;
        }

        for (int i = 0; i < team.size(); i++) {
            TeamEntry otherEntry = trip.team.get(i);
            TeamEntry thisEntry = team.get(i);
            if (!thisEntry.equals(otherEntry)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (surveyDate != null ? surveyDate.hashCode() : 0);
        result = 31 * result + (explorationDate != null ? explorationDate.hashCode() : 0);
        result = 31 * result + (explorationDateLinked ? 1 : 0);
        result = 31 * result + (comments != null ? comments.hashCode() : 0);
        result = 31 * result + (instrument != null ? instrument.hashCode() : 0);
        result = 31 * result + team.hashCode();
        return result;
    }

    private static boolean objectsNotEqual(Object a, Object b) {
        if (a == null) return b != null;
        return !a.equals(b);
    }
}
