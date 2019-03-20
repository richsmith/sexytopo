package org.hwyl.sexytopo.model.survey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Trip {

    public enum Role {
        BOOK("Book (drawing)"),
        INSTRUMENTS("Instruments"),
        DOG("Dog (assistant)"),
        EXPLORATION("Exploration Team");

        public String description;
        Role(String description) {
            this.description = description;
        }

        public String toString() {
            return description;
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
    }

    private Date date;
    private List<TeamEntry> team = new ArrayList<>();
    private String comments;


    public Trip() {
        this.date = new Date();
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

    public boolean equalsTripData(Trip trip) {
        // Note, not "equals" method because the trips could have same data but not the same ID

        if (! trip.date.equals(date)) {
            return false;
        }

        if (! trip.comments.equals(comments)) {
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

}
