package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

import java.util.Map;

/**
 * Created by rls on 09/09/15.
 */
public class SpaceMover {
    public static Space<Coord2D> move(Space<Coord2D> space, Coord2D delta) {
        Space moved = new Space<Coord2D>();

        Map<Station, Coord2D> stationMap = space.getStationMap();
        for (Station station : stationMap.keySet()) {
            Coord2D point = stationMap.get(station);
            moved.addStation(station, point.plus(delta));
        }


        Map<Leg, Line<Coord2D>> legMap = space.getLegMap();
        for (Leg leg : legMap.keySet()) {
            Line<Coord2D> line = space.getLegMap().get(leg);
            Line<Coord2D> shiftedLine =
                    new Line<>(line.getStart().plus(delta), line.getEnd().plus(delta));
            moved.addLeg(leg, shiftedLine);
        }
        

        return moved;
    }
}
