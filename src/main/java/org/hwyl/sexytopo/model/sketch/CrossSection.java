package org.hwyl.sexytopo.model.sketch;

import android.graphics.Color;

import org.hwyl.sexytopo.control.util.Space3DTransformer;
import org.hwyl.sexytopo.control.util.StationRotator;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a cross-section of a passage at a specified angle on a top-down plan sketch.
 */
public class CrossSection extends SketchDetail {

    private Station station;
    private double angle;

    private static Space3DTransformer transformer = new Space3DTransformer();

    public CrossSection(Station station, double angle) {
        super(Colour.NONE);
        this.station = station;
        this.angle = angle;
    }

    public Space<Coord2D> getProjection() {

        Space<Coord2D> projection = new Space<>();
        projection.addStation(station, Coord2D.ORIGIN);

        for (Leg leg : getSplays(station)) {

            Leg rotated = leg.rotate(angle);
            Coord3D coord3D = transformer.transform(Coord3D.ORIGIN, rotated);
            Coord2D coord2D = new Coord2D(coord3D.getX(), coord3D.getZ());
            Line<Coord2D> line = new Line<>(Coord2D.ORIGIN, coord2D);
            projection.addLeg(rotated, line);
        }

        return projection;
    }

    private static Set<Leg> getSplays(Station station) {
        Set<Leg> legs = new HashSet<>(station.getOnwardLegs());
        legs.removeAll(station.getConnectedOnwardLegs());
        return legs;
    }

    public Station getStation() {
        return station;
    }
}
