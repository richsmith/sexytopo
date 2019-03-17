package org.hwyl.sexytopo.model.survey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Trip {

    public enum Role {
        BOOK("Book (drawing)"),
        INSTRUMENTS("Instruments"),
        DOG("Dog"),
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
    }

    private int id;
    private Date date;
    private List<TeamEntry> team = new ArrayList<>();
    private String comments;


    public Trip(int id) {
        this.id = id;
        this.date = new Date();
    }


    public int getId() {
        return id;
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
    

}
