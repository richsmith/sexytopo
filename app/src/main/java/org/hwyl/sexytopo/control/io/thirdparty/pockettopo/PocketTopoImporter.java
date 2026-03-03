package org.hwyl.sexytopo.control.io.thirdparty.pockettopo;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.SurveyUpdater;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Import for the native PocketTopo binary .top file format (version 3).
 * See docs/PocketTopoFileFormat.txt for the format specification.
 */
public class PocketTopoImporter extends Importer {

    @Override
    public Survey toSurvey(Context context, DocumentFile file) throws Exception {
        try (InputStream raw = context.getContentResolver().openInputStream(file.getUri());
             BufferedInputStream in = new BufferedInputStream(raw)) {
            return parseSurvey(in);
        }
    }

    @Override
    public boolean canHandleFile(DocumentFile file) {
        return file.isFile() && file.getName() != null && file.getName().endsWith("top");
    }


    static Survey parseSurvey(InputStream in) throws IOException {
        readAndVerifyHeader(in);

        List<TripData> trips = readTrips(in);
        List<ShotData> shots = readShots(in);
        List<ReferenceData> references = readReferences(in);

        readMapping(in); // overview mapping - skip

        DrawingData planDrawing = readDrawing(in);
        DrawingData elevationDrawing = readDrawing(in);

        Survey survey = buildSurvey(trips, shots, references);

        survey.setPlanSketch(buildSketch(planDrawing));
        survey.setElevationSketch(buildSketch(elevationDrawing));
        survey.setSaved(true);

        return survey;
    }


    // --- Internal data classes ---

    static class TripData {
        Date date;
        String comment;
        float declination;
    }

    static class ShotData {
        String from;
        String to;
        float distance;
        float azimuth;
        float inclination;
        boolean flipped;
        int tripIndex;
        String comment;
    }

    static class ReferenceData {
        String station;
        long east;
        long north;
        int altitude;
        String comment;
    }

    static class DrawingData {
        List<PolygonData> polygons = new ArrayList<>();
    }

    static class PolygonData {
        Colour colour;
        List<Coord2D> points = new ArrayList<>();
    }


    // --- Header ---

    private static void readAndVerifyHeader(InputStream in) throws IOException {
        int t = PocketTopoFile.readByte(in);
        int o = PocketTopoFile.readByte(in);
        int p = PocketTopoFile.readByte(in);
        int version = PocketTopoFile.readByte(in);
        if (t != 'T' || o != 'o' || p != 'p' || version != 3) {
            throw new IOException(
                "Not a valid PocketTopo file (expected header 'Top\\3', got '"
                + (char) t + (char) o + (char) p + "'\\" + version + ")");
        }
    }


    // --- Reading structured data ---

    private static List<TripData> readTrips(InputStream in) throws IOException {
        int count = PocketTopoFile.readInt32(in);
        List<TripData> trips = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            TripData trip = new TripData();
            long ticks = PocketTopoFile.readInt64(in);
            trip.date = PocketTopoFile.ticksToDate(ticks);
            trip.comment = PocketTopoFile.readString(in);
            short rawDeclination = PocketTopoFile.readInt16(in);
            trip.declination = PocketTopoFile.azimuthToDegrees(rawDeclination);
            trips.add(trip);
        }
        return trips;
    }

    private static List<ShotData> readShots(InputStream in) throws IOException {
        int count = PocketTopoFile.readInt32(in);
        List<ShotData> shots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ShotData shot = new ShotData();
            shot.from = PocketTopoFile.readId(in);
            shot.to = PocketTopoFile.readId(in);
            shot.distance = PocketTopoFile.distanceToMetres(PocketTopoFile.readInt32(in));
            shot.azimuth = PocketTopoFile.azimuthToDegrees(PocketTopoFile.readInt16(in));
            shot.inclination = PocketTopoFile.inclinationToDegrees(PocketTopoFile.readInt16(in));
            int flags = PocketTopoFile.readByte(in);
            shot.flipped = (flags & 1) != 0;
            PocketTopoFile.readByte(in); // roll - not used
            shot.tripIndex = PocketTopoFile.readInt16(in);
            if ((flags & 2) != 0) {
                shot.comment = PocketTopoFile.readString(in);
            }
            shots.add(shot);
        }
        return shots;
    }

    private static List<ReferenceData> readReferences(InputStream in) throws IOException {
        int count = PocketTopoFile.readInt32(in);
        List<ReferenceData> refs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ReferenceData ref = new ReferenceData();
            ref.station = PocketTopoFile.readId(in);
            ref.east = PocketTopoFile.readInt64(in);
            ref.north = PocketTopoFile.readInt64(in);
            ref.altitude = PocketTopoFile.readInt32(in);
            ref.comment = PocketTopoFile.readString(in);
            refs.add(ref);
        }
        return refs;
    }

    private static void readMapping(InputStream in) throws IOException {
        PocketTopoFile.readInt32(in); // origin x
        PocketTopoFile.readInt32(in); // origin y
        PocketTopoFile.readInt32(in); // scale
    }

    private static DrawingData readDrawing(InputStream in) throws IOException {
        DrawingData drawing = new DrawingData();
        readMapping(in); // each drawing has its own mapping
        while (true) {
            int elementId = PocketTopoFile.readByte(in);
            if (elementId == 0) {
                break;
            }
            switch (elementId) {
                case 1: // PolygonElement
                    drawing.polygons.add(readPolygon(in));
                    break;
                case 3: // XSectionElement
                    skipXSection(in);
                    break;
                default:
                    throw new IOException("Unknown drawing element type: " + elementId);
            }
        }
        return drawing;
    }

    private static PolygonData readPolygon(InputStream in) throws IOException {
        PolygonData polygon = new PolygonData();
        int pointCount = PocketTopoFile.readInt32(in);
        for (int i = 0; i < pointCount; i++) {
            int x = PocketTopoFile.readInt32(in);
            int y = PocketTopoFile.readInt32(in);
            // Convert mm to metres
            polygon.points.add(new Coord2D(x / 1000.0f, y / 1000.0f));
        }
        int colourByte = PocketTopoFile.readByte(in);
        polygon.colour = PocketTopoFile.topoColourToColour(colourByte);
        return polygon;
    }

    private static void skipXSection(InputStream in) throws IOException {
        PocketTopoFile.readInt32(in); // pos x
        PocketTopoFile.readInt32(in); // pos y
        PocketTopoFile.readId(in);    // station
        PocketTopoFile.readInt32(in); // direction
    }


    // --- Survey building ---

    /**
     * Build a Survey from parsed shot data.
     *
     * Uses multi-pass processing because shots in .top files are stored in recording
     * order, not tree-traversal order. A shot referencing a "from" station that hasn't
     * been created yet is deferred to a later pass.
     *
     * Adds legs directly to stations rather than using SurveyUpdater, to avoid
     * triple-shot detection creating unwanted auto-named stations during import.
     */
    private static Survey buildSurvey(
            List<TripData> trips, List<ShotData> shots, List<ReferenceData> references) {

        Survey survey = new Survey();

        if (!trips.isEmpty()) {
            Trip trip = new Trip();
            trip.setDate(trips.get(0).date);
            trip.setComments(trips.get(0).comment);
            survey.setTrip(trip);
        }

        // Find the first valid "from" station to use as origin
        for (ShotData shot : shots) {
            if (shot.from != null) {
                survey.getOrigin().setName(shot.from);
                break;
            }
        }

        // Process shots in multiple passes until no more progress can be made.
        // Each pass processes shots whose "from" station already exists in the tree.
        boolean[] processed = new boolean[shots.size()];
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < shots.size(); i++) {
                if (processed[i]) {
                    continue;
                }

                ShotData shot = shots.get(i);

                if (shot.from == null) {
                    processed[i] = true;
                    progress = true;
                    continue;
                }

                Station fromStation = survey.getStationByName(shot.from);
                Station toStation = (shot.to != null)
                        ? survey.getStationByName(shot.to) : null;

                if (shot.to == null) {
                    // Splay leg — needs "from" station to exist
                    if (fromStation == null) {
                        continue; // defer
                    }
                    processed[i] = true;
                    progress = true;
                    Leg leg = new Leg(shot.distance, shot.azimuth, shot.inclination);
                    fromStation.addOnwardLeg(leg);
                    survey.addLegRecord(leg);

                } else if (fromStation != null && toStation == null) {
                    // Foresight: "from" exists, "to" is new
                    // Collect all repeat shots for this station pair and average
                    List<Leg> originalLegs = collectRepeatLegs(shots, processed, shot);
                    progress = true;

                    Station newStation = new Station(shot.to);
                    if (shot.comment != null && !shot.comment.isEmpty()) {
                        newStation.setComment(shot.comment);
                    }
                    newStation.setExtendedElevationDirection(
                            shot.flipped ? Direction.LEFT : Direction.RIGHT);

                    Leg averaged = (originalLegs.size() > 1)
                            ? SurveyUpdater.averageLegs(originalLegs)
                            : originalLegs.get(0);
                    Leg[] promotedFrom = (originalLegs.size() > 1)
                            ? originalLegs.toArray(new Leg[0])
                            : new Leg[]{};

                    Leg leg = new Leg(averaged.getDistance(), averaged.getAzimuth(),
                            averaged.getInclination(), newStation, promotedFrom);
                    fromStation.addOnwardLeg(leg);
                    survey.addLegRecord(leg);
                    survey.setActiveStation(newStation);

                } else if (fromStation == null && toStation != null) {
                    // Backsight: "to" exists, "from" is new.
                    // Collect repeats, average, then convert to forward direction
                    // matching SurveyUpdater convention.
                    List<Leg> originalLegs = collectRepeatLegs(shots, processed, shot);
                    progress = true;

                    Station newStation = new Station(shot.from);
                    if (shot.comment != null && !shot.comment.isEmpty()) {
                        newStation.setComment(shot.comment);
                    }
                    newStation.setExtendedElevationDirection(
                            shot.flipped ? Direction.LEFT : Direction.RIGHT);

                    Leg averaged = (originalLegs.size() > 1)
                            ? SurveyUpdater.averageLegs(originalLegs)
                            : originalLegs.get(0);
                    Leg[] promotedFrom = (originalLegs.size() > 1)
                            ? originalLegs.toArray(new Leg[0])
                            : new Leg[]{};

                    float forwardAzimuth = Space2DUtils.adjustAngle(
                            averaged.getAzimuth(), 180);
                    float forwardInclination = -averaged.getInclination();
                    Leg leg = new Leg(averaged.getDistance(), forwardAzimuth,
                            forwardInclination, newStation, promotedFrom, true);
                    toStation.addOnwardLeg(leg);
                    survey.addLegRecord(leg);
                    survey.setActiveStation(newStation);

                } else if (fromStation != null) {
                    // Both exist — loop closure, skip
                    processed[i] = true;
                    progress = true;

                } else {
                    continue; // neither exists — defer to next pass
                }
            }
        }

        // Apply reference comments to stations
        for (ReferenceData ref : references) {
            if (ref.station != null && ref.comment != null && !ref.comment.isEmpty()) {
                Station station = survey.getStationByName(ref.station);
                if (station != null) {
                    station.setComment(ref.comment);
                }
            }
        }

        return survey;
    }

    /**
     * Collect all unprocessed shots with the same (from, to) station pair,
     * convert to splay Legs, and mark them as processed.
     */
    private static List<Leg> collectRepeatLegs(
            List<ShotData> shots, boolean[] processed, ShotData target) {
        List<Leg> legs = new ArrayList<>();
        for (int j = 0; j < shots.size(); j++) {
            if (processed[j]) continue;
            ShotData s = shots.get(j);
            if (target.from.equals(s.from) && target.to.equals(s.to)) {
                legs.add(new Leg(s.distance, s.azimuth, s.inclination));
                processed[j] = true;
            }
        }
        return legs;
    }

    private static Sketch buildSketch(DrawingData drawingData) {
        Sketch sketch = new Sketch();
        List<PathDetail> pathDetails = new ArrayList<>();

        for (PolygonData polygon : drawingData.polygons) {
            if (polygon.points.isEmpty()) {
                continue;
            }

            // PocketTopo .top uses screen-oriented coordinates (Y increases downward)
            Coord2D first = polygon.points.get(0);
            PathDetail pathDetail = new PathDetail(first, polygon.colour);
            for (int i = 1; i < polygon.points.size(); i++) {
                pathDetail.lineTo(polygon.points.get(i));
            }
            pathDetails.add(pathDetail);
        }

        sketch.setPathDetails(pathDetails);
        return sketch;
    }
}
