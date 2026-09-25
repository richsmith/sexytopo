package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.util.Space3DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

/**
 * Represents a cross-section of a passage at a station. It is either a vertical plane facing a
 * compass direction (the angle), or a horizontal slice through the station. A horizontal slice
 * always lies flat in the real world, drawn with north at the top, so it has no angle to choose.
 */
public class CrossSection {

    public enum Orientation {
        VERTICAL,
        HORIZONTAL
    }

    private final Station station;
    private final float angle;
    private final Orientation orientation;

    /** A vertical cross-section facing the given compass azimuth (in degrees). */
    public CrossSection(Station station, float angle) {
        this(station, angle, Orientation.VERTICAL);
    }

    private CrossSection(Station station, float angle, Orientation orientation) {
        this.station = station;
        this.angle = angle;
        this.orientation = orientation;
    }

    /** A horizontal cross-section: a slice lying flat through the station. */
    public static CrossSection horizontal(Station station) {
        return new CrossSection(station, 0f, Orientation.HORIZONTAL);
    }

    public Space<Coord2D> getProjection() {

        Space<Coord2D> projection = new Space<>();
        projection.addStation(station, Coord2D.ORIGIN);

        Projection2D projectionType = getProjectionType();

        for (Leg leg : station.getUnconnectedOnwardLegs()) {
            // A vertical cross-section is first normalised to match its angle; a horizontal one
            // already lies in the real-world horizontal plane so is used as it is.
            Leg drawn = isRotatable() ? leg.rotate(-angle) : leg;
            Coord3D coord3D = Space3DUtils.toCartesian(Coord3D.ORIGIN, drawn);
            Coord2D coord2D = projectionType.project(coord3D);
            Line<Coord2D> line = new Line<>(Coord2D.ORIGIN, coord2D);
            projection.addLeg(drawn, line);
        }

        return projection;
    }

    /**
     * The projection used to flatten this cross-section's splays into the sketch of the section
     * itself: as seen looking along the way it faces, with up as up, for a vertical one, and as
     * seen looking down, with north up the page
     */
    public Projection2D getProjectionType() {
        return orientation == Orientation.HORIZONTAL
                ? Projection2D.PLAN
                : Projection2D.CROSS_SECTION;
    }

    public Station getStation() {
        return station;
    }

    /** The compass azimuth a vertical cross-section faces. Always 0 for a horizontal one. */
    public float getAngle() {
        return angle;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    /** Whether the cross-section has a direction that can be set. Only vertical ones do. */
    public boolean isRotatable() {
        return orientation == Orientation.VERTICAL;
    }
}
