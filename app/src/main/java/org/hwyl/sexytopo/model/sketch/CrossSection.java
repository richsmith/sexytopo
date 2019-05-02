package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.util.Space3DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

/**
 * Represents a cross-section of a passage at a specified angle on a top-down plan sketch.
 */
public class CrossSection {

    private final Station station;
    private final double angle;

    public CrossSection(Station station, double angle) {
        this.station = station;
        this.angle = angle;
    }

    public Space<Coord2D> getProjection() {

        Space<Coord2D> projection = new Space<>();
        projection.addStation(station, Coord2D.ORIGIN);

        for (Leg leg : station.getUnconnectedOnwardLegs()) {
            // first of all normalise to match the angle of the cross section
            Leg rotated = leg.rotate(-angle);
            Coord3D coord3D = Space3DUtils.toCartesian(Coord3D.ORIGIN, rotated);
            Coord2D coord2D = new Coord2D(coord3D.x, coord3D.z);
            Line<Coord2D> line = new Line<>(Coord2D.ORIGIN, coord2D);
            projection.addLeg(rotated, line);
        }

        return projection;
    }

    public Station getStation() {
        return station;
    }

    public double getAngle() {
        return angle;
    }
}
