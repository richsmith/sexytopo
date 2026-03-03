package org.hwyl.sexytopo.model.graph;

import org.hwyl.sexytopo.control.util.Space3DTransformer;
import org.hwyl.sexytopo.control.util.Space3DTransformerForElevation;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Map;


@SuppressWarnings("SuspiciousNameCombination")
public enum Projection2D {

    // The projections flip the space vertically (inverts the y-axis) because mathematically
    // we consider the bottom left to be 0,0 but on the screen we consider the top left to be 0,0
    // I think it makes sense to do it here (might reduce calculations a tiny bit compared to doing
    // it in the GraphView)...
    // We just have to remember to reverse the flip when exporting the sketch :)

    PLAN("Plan", "plan") {
        public Coord2D project(Coord3D coord3D) {
            return new Coord2D(coord3D.x, -coord3D.y);
        }

        public boolean isLegInPlane(Leg leg) {
            return -45 < leg.getInclination() && leg.getInclination() < 45;
        }
    },
    ELEVATION_NS("Elevation NS", "elev_ns") {
        public Coord2D project(Coord3D coord3D) {
            return new Coord2D(coord3D.y, -coord3D.z);
        }

        public boolean isLegInPlane(Leg leg) {
            return true;
        }
    },
    ELEVATION_EW("Elevation EW", "elev_ew") {
        public Coord2D project(Coord3D coord3D) {
            return new Coord2D(coord3D.x, -coord3D.z);
        }

        public boolean isLegInPlane(Leg leg) {
            return true;
        }
    },
    EXTENDED_ELEVATION("Extended Elevation", "ee") {
        public Coord2D project(Coord3D coord3D) {
            return ELEVATION_NS.project(coord3D);
        }

        public boolean isLegInPlane(Leg leg) {
            return true;
        }
    };

    private static final Space3DTransformer space3DTransformer =
            new Space3DTransformer();
    private static final Space3DTransformerForElevation space3DTransformerForElevation =
            new Space3DTransformerForElevation();
    private final String name;
    private final String abbreviation;

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    Projection2D(String name, String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public Space<Coord3D> transform(Survey survey) {
        if (this == EXTENDED_ELEVATION) {
            return space3DTransformerForElevation.transformTo3D(survey);
        } else {
            return space3DTransformer.transformTo3D(survey);
        }
    }

    public abstract Coord2D project(Coord3D coord3D);
    public abstract boolean isLegInPlane(Leg leg);

    public Space<Coord2D> project(Survey survey) {

        Space<Coord3D> space3D = transform(survey);

        Space<Coord2D> space2D = new Space<>();

        for (Map.Entry<Station, Coord3D> entry : space3D.getStationMap().entrySet()) {
            Station station = entry.getKey();
            Coord3D stationCoord3D = entry.getValue();
            Coord2D stationCoord2D = project(stationCoord3D);
            space2D.addStation(station, stationCoord2D);
        }

        for (Map.Entry<Leg, Line<Coord3D>> entry : space3D.getLegMap().entrySet()) {
            Leg leg = entry.getKey();
            Line<Coord3D> line3D = entry.getValue();
            Coord2D start = project(line3D.getStart());
            Coord2D end = project(line3D.getEnd());
            Line<Coord2D> line2D = new Line<>(start, end);
            space2D.addLeg(leg, line2D);
        }

        return space2D;
    }

}
