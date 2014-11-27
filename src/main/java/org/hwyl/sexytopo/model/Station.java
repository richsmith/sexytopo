package org.hwyl.sexytopo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by rls on 16/07/14.
 */
public class Station {

    private final String name;
    private List<Leg> onwardLegs = new ArrayList<>();

    public Station(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Leg> getOnwardLegs() {
        return onwardLegs;
    }

    public void addOnwardLeg(Leg leg) {
        onwardLegs.add(leg);
    }

    public List<Leg> getConnectedOnwardLegs() {
        List<Leg> connectedOnwardLegs = new ArrayList<>();
        for (Leg leg : onwardLegs) {
            if (leg.hasDestination()) {
                connectedOnwardLegs.add(leg);
            }
        }
        return connectedOnwardLegs;
    }

    public String toString() {
        return name;
    }
}
