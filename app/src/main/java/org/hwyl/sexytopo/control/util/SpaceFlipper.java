package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

import java.util.Map;


public class SpaceFlipper {

    @SuppressWarnings("ConstantConditions")
    public static Space<Coord2D> flipVertically(Space<Coord2D> space) {

        Space<Coord2D> flippedSpace = new Space<>();

        Map<Station, Coord2D> stationMap = space.getStationMap();
        for (Station station : stationMap.keySet()) {
            Coord2D point = stationMap.get(station);
            flippedSpace.addStation(station, point.flipVertically());
        }

        Map<Leg, Line<Coord2D>> legMap = space.getLegMap();
        for (Leg leg : legMap.keySet()) {
            Line<Coord2D> line = legMap.get(leg);
            Coord2D start = line.getStart();
            Coord2D end = line.getEnd();
            Line<Coord2D> flippedLine = new Line<>(start.flipVertically(), end.flipVertically());
            flippedSpace.addLeg(leg, flippedLine);
        }

        return flippedSpace;
    }
}
