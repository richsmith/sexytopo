package org.hwyl.sexytopo.testutils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.Space3DUtils;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.LRUD;

/**
 * Creates a test survey with a random number of branches and stations. Unlike the other test utils
 * in this package, this is expected to be called from user-facing code, not from tests.
 */
public class ExampleSurveyCreator {

    private static final Random random = new Random();

    // number of stations to get a cross-section when requested
    private static final float X_SECTION_FRACTION = 0.25f;

    /** Probability that any given station gets a symbol nearby when addSymbols is on. */
    private static final float SYMBOL_PROBABILITY = 0.5f;

    /** Maximum offset (metres) of a generated symbol from its station. */
    private static final float SYMBOL_OFFSET_RADIUS = 1.0f;

    /** Default symbol size (metres). Lands in the "small" Therion size bucket. */
    private static final float SYMBOL_SIZE = 0.5f;

    /** Maximum sideways jitter on a freehand sub-segment, as a fraction of segment length. */
    private static final float FREEHAND_JITTER = 0.03f;

    public static Survey create() {
        return create(10, 5, false, false, false);
    }

    public static Survey create(int numStations, int numBranches) {
        return create(numStations, numBranches, false, false, false);
    }

    public static Survey create(int numStations, int numBranches, boolean addCrossSections) {
        return create(numStations, numBranches, addCrossSections, false, false);
    }

    public static Survey create(
            int numStations, int numBranches, boolean addCrossSections, boolean addSketchLines) {
        return create(numStations, numBranches, addCrossSections, addSketchLines, false);
    }

    public static Survey create(
            int numStations,
            int numBranches,
            boolean addCrossSections,
            boolean addSketchLines,
            boolean addSymbols) {

        Log.i("Generating example survey");

        Survey survey = new Survey();

        createBranch(survey, numStations);

        for (int i = 0; i < numBranches; i++) {
            List<Station> stations = survey.getAllStations();
            stations.remove(survey.getOrigin());
            if (stations.isEmpty()) {
                break;
            }
            Station active = getRandom(stations);
            survey.setActiveStation(active);
            createBranch(survey, 3);
        }

        if (addCrossSections) {
            addCrossSections(survey);
        }

        if (addSketchLines) {
            addSketchLines(survey);
        }

        if (addSymbols) {
            addSymbols(survey);
        }

        return survey;
    }

    private static void addSymbols(Survey survey) {
        Sketch plan = survey.getSketch(Projection2D.PLAN);
        Map<Station, Coord2D> stationPositions = Projection2D.PLAN.project(survey).getStationMap();
        Symbol[] symbols = Symbol.values();
        for (Coord2D stationPos : stationPositions.values()) {
            if (random.nextFloat() >= SYMBOL_PROBABILITY) {
                continue;
            }
            float dx = (random.nextFloat() * 2 - 1) * SYMBOL_OFFSET_RADIUS;
            float dy = (random.nextFloat() * 2 - 1) * SYMBOL_OFFSET_RADIUS;
            Symbol symbol = symbols[random.nextInt(symbols.length)];
            float angle = symbol.isDirectional() ? random.nextFloat() * 360 : 0;
            plan.addSymbolDetail(stationPos.add(dx, dy), symbol, SYMBOL_SIZE, angle);
        }
    }

    private static void addCrossSections(Survey survey) {
        Sketch plan = survey.getSketch(Projection2D.PLAN);
        Map<Station, Coord2D> stationPositions = Projection2D.PLAN.project(survey).getStationMap();
        List<Station> candidates = survey.getAllStations();
        candidates.remove(survey.getOrigin());
        int count = Math.max(1, Math.round(candidates.size() * X_SECTION_FRACTION));
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            Station station = candidates.remove(random.nextInt(candidates.size()));
            CrossSection xs = CrossSectioner.section(survey, station);
            float radius = CrossSectioner.getHorizontalRadius(station);
            float offset = (radius > 0 ? radius : 3) * 2.5f;
            Coord2D position = stationPositions.get(station).add(0, offset);
            plan.addCrossSection(xs, position);
        }
    }

    /**
     * Draw left- and right-wall polylines that follow the survey's branching tree. Each linear
     * chain becomes one continuous path; at a fork, every child branch starts a fresh path from the
     * shared parent wall point so the walls visibly meet at the junction. Also draws a closed wall
     * outline inside any cross-section's sub-sketch by joining its splay endpoints. Gives the
     * th2/xvi export real wall-shaped content to round-trip.
     */
    private static void addSketchLines(Survey survey) {
        Sketch plan = survey.getSketch(Projection2D.PLAN);
        Map<Station, Coord2D> stationPositions = Projection2D.PLAN.project(survey).getStationMap();
        for (LRUD side : new LRUD[] {LRUD.LEFT, LRUD.RIGHT}) {
            Map<Station, Coord2D> wallPoints = new HashMap<>();
            Station origin = survey.getOrigin();
            Coord2D originWall = wallPoint(survey, stationPositions, origin, side);
            if (originWall == null) {
                continue;
            }
            wallPoints.put(origin, originWall);
            drawWallChain(survey, plan, stationPositions, wallPoints, origin, side, null);
        }
        for (CrossSectionDetail xsDetail : plan.getCrossSectionDetails()) {
            drawCrossSectionWalls(xsDetail);
        }
    }

    /**
     * Walk the survey tree from `from`, extending `path` with `lineTo` along each linear segment.
     * At a branch, recurse with a fresh path per child starting at the shared parent wall point.
     * `path` is null at the very start (a new path is opened for the first leg) and at every
     * recursion entry into a branch.
     */
    private static void drawWallChain(
            Survey survey,
            Sketch plan,
            Map<Station, Coord2D> stationPositions,
            Map<Station, Coord2D> wallPoints,
            Station from,
            LRUD side,
            PathDetail path) {
        List<Leg> onward = from.getConnectedOnwardLegs();
        Coord2D fromWall = wallPoints.get(from);
        if (fromWall == null) {
            return;
        }
        if (onward.size() == 1) {
            Leg leg = onward.get(0);
            Station child = leg.getDestination();
            Coord2D childWall =
                    wallPoints.computeIfAbsent(
                            child, s -> wallPoint(survey, stationPositions, s, side));
            if (childWall == null) {
                return;
            }
            if (path == null) {
                path = plan.startNewPath(fromWall);
            }
            toFreehand(path, fromWall, childWall);
            drawWallChain(survey, plan, stationPositions, wallPoints, child, side, path);
        } else {
            for (Leg leg : onward) {
                Station child = leg.getDestination();
                Coord2D childWall =
                        wallPoints.computeIfAbsent(
                                child, s -> wallPoint(survey, stationPositions, s, side));
                if (childWall == null) {
                    continue;
                }
                PathDetail branch = plan.startNewPath(fromWall);
                toFreehand(branch, fromWall, childWall);
                drawWallChain(survey, plan, stationPositions, wallPoints, child, side, branch);
            }
        }
    }

    /**
     * Append the segment `from`->`to` to `path` as 6-12 straight sub-segments, with each interior
     * joint nudged in a small random direction. `from` is assumed to be already on the path; only
     * the interior points and `to` are appended. Endpoints stay exact so adjacent segments still
     * meet at stations.
     */
    private static PathDetail toFreehand(PathDetail path, Coord2D from, Coord2D to) {
        int subSegments = 6 + random.nextInt(7);
        Coord2D delta = to.minus(from);
        float length = delta.mag();
        float maxOffset = length * FREEHAND_JITTER;
        for (int i = 1; i < subSegments; i++) {
            float t = (float) i / subSegments;
            Coord2D straight = from.plus(delta.scale(t));
            float dx = (random.nextFloat() * 2 - 1) * maxOffset;
            float dy = (random.nextFloat() * 2 - 1) * maxOffset;
            path.lineTo(straight.add(dx, dy));
        }
        path.lineTo(to);
        return path;
    }

    /**
     * Draw a closed wall outline inside the cross-section's sub-sketch by sorting splay endpoints
     * around the station origin and connecting them in angular order.
     */
    private static void drawCrossSectionWalls(CrossSectionDetail xsDetail) {
        List<Coord2D> endpoints = new ArrayList<>();
        for (Line<Coord2D> line : xsDetail.getCrossSection().getProjection().getLegMap().values()) {
            endpoints.add(line.getEnd());
        }
        if (endpoints.size() < 2) {
            return;
        }
        endpoints.sort(Comparator.comparingDouble(p -> Math.atan2(p.y, p.x)));
        Sketch sub = xsDetail.getSketch();
        PathDetail path = sub.startNewPath(endpoints.get(0));
        for (int i = 1; i < endpoints.size(); i++) {
            toFreehand(path, endpoints.get(i - 1), endpoints.get(i));
        }
        toFreehand(path, endpoints.get(endpoints.size() - 1), endpoints.get(0));
    }

    private static Coord2D wallPoint(
            Survey survey, Map<Station, Coord2D> positions, Station station, LRUD side) {
        Coord2D pos = positions.get(station);
        if (pos == null) {
            return null;
        }
        float distance = 1 + random.nextFloat() * 3;
        Leg splay = side.createSplay(survey, station, LRUD.Mode.SURVEY, distance);
        // pos is already plan-projected (y is flipped vs. the survey frame). Project the splay
        // on its own, starting from the survey origin, then add the offset to pos.
        Coord2D splayOffset =
                Projection2D.PLAN.project(Space3DUtils.toCartesian(Coord3D.ORIGIN, splay));
        return pos.plus(splayOffset);
    }

    public static void createBranch(Survey survey, int numStations) {

        for (int i = 0; i < numStations; i++) {

            float distance = 5 + random.nextInt(10);
            float azimuth = 40 + random.nextInt(100);
            float inclination = -20 + random.nextInt(40);

            Leg leg = new Leg(distance, azimuth, inclination);
            SurveyUpdater.updateWithNewStation(survey, leg);

            Station newStation = survey.getMostRecentLeg().getDestination();
            createLruds(survey, newStation);
        }
    }

    public static void createLruds(Survey survey, Station station) {
        for (LRUD lrud : LRUD.values()) {
            Leg splay = lrud.createSplay(survey, station, LRUD.Mode.SURVEY, 1 + random.nextInt(3));
            SurveyUpdater.update(survey, splay);
        }
    }

    public static <T> T getRandom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
