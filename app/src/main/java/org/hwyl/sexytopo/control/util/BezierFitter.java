package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/**
 * Fits a chain of cubic Bézier curves through a polyline, to within a given distance of it. This is
 * Philip Schneider's algorithm from Graphics Gems ("An Algorithm for Automatically Fitting
 * Digitized Curves"): fit one curve by least squares, nudge the parameterisation with
 * Newton-Raphson if it's nearly good enough, and otherwise split at the worst point and recurse.
 *
 * <p>The line is first broken at sharp corners, which are kept as corners rather than rounded off.
 * Curves joining at any other point share a tangent, so the chain is smooth there.
 */
public final class BezierFitter {

    /** A turn sharper than this at a point (in degrees) is kept as a corner. */
    private static final double CORNER_ANGLE_DEGREES = 60;

    private static final int MAX_REPARAMETERISATIONS = 4;

    private BezierFitter() {}

    /** One cubic Bézier segment: start, two control points, end. */
    public static final class CubicBezier {
        public final Coord2D start;
        public final Coord2D control1;
        public final Coord2D control2;
        public final Coord2D end;

        public CubicBezier(Coord2D start, Coord2D control1, Coord2D control2, Coord2D end) {
            this.start = start;
            this.control1 = control1;
            this.control2 = control2;
            this.end = end;
        }

        public Coord2D pointAt(float t) {
            float mt = 1 - t;
            float b0 = mt * mt * mt;
            float b1 = 3 * mt * mt * t;
            float b2 = 3 * mt * t * t;
            float b3 = t * t * t;
            return new Coord2D(
                    b0 * start.x + b1 * control1.x + b2 * control2.x + b3 * end.x,
                    b0 * start.y + b1 * control1.y + b2 * control2.y + b3 * end.y);
        }
    }

    /**
     * Fits curves to the line so that no part of it is further than tolerance from the chain. Each
     * curve starts where the previous one ends. Returns an empty list for fewer than two distinct
     * points.
     */
    public static List<CubicBezier> fit(List<Coord2D> points, float tolerance) {
        // the fit is only checked at the points, so a long straight run between two of them would
        // leave the curve free to wander off it: fill such runs in with points a tolerance apart
        List<Coord2D> distinct = fillIn(removeRepeats(points), tolerance);
        List<CubicBezier> curves = new ArrayList<>();
        if (distinct.size() < 2) {
            return curves;
        }

        boolean isClosed = distinct.size() > 3 && distinct.get(0).equals(last(distinct));

        int runStart = 0;
        for (int i = 1; i < distinct.size() - 1; i++) {
            if (isCorner(distinct, i)) {
                fitRun(distinct, runStart, i, isClosed, tolerance, curves);
                runStart = i;
            }
        }
        fitRun(distinct, runStart, distinct.size() - 1, isClosed, tolerance, curves);
        return curves;
    }

    private static void fitRun(
            List<Coord2D> points,
            int first,
            int last,
            boolean isClosed,
            float tolerance,
            List<CubicBezier> curves) {
        int end = points.size() - 1;
        Coord2D startTangent = points.get(first + 1).minus(points.get(first)).normalise();
        Coord2D endTangent = points.get(last - 1).minus(points.get(last)).normalise();

        // a smooth closed loop shouldn't get a kink where it was started and finished
        if (isClosed && !isCorner(points.get(end - 1), points.get(0), points.get(1))) {
            Coord2D joinTangent = points.get(1).minus(points.get(end - 1)).normalise();
            if (joinTangent.mag() > 0) {
                if (first == 0) {
                    startTangent = joinTangent;
                }
                if (last == end) {
                    endTangent = joinTangent.scale(-1);
                }
            }
        }

        fitCubic(points, first, last, startTangent, endTangent, tolerance * tolerance, curves);
    }

    /**
     * The tangents point into the run: startTangent away from the first point, endTangent back from
     * the last one.
     */
    private static void fitCubic(
            List<Coord2D> points,
            int first,
            int last,
            Coord2D startTangent,
            Coord2D endTangent,
            float toleranceSquared,
            List<CubicBezier> curves) {

        if (last - first == 1) {
            float third = distance(points.get(first), points.get(last)) / 3;
            curves.add(
                    new CubicBezier(
                            points.get(first),
                            points.get(first).plus(startTangent.scale(third)),
                            points.get(last).plus(endTangent.scale(third)),
                            points.get(last)));
            return;
        }

        float[] params = chordLengthParameterise(points, first, last);
        CubicBezier curve = generateBezier(points, first, last, params, startTangent, endTangent);
        int[] splitPoint = new int[1];
        float maxError = getMaxErrorSquared(points, first, last, curve, params, splitPoint);
        if (maxError < toleranceSquared) {
            curves.add(curve);
            return;
        }

        // nearly there: improving the parameterisation can often save a split
        if (maxError < toleranceSquared * 4) {
            for (int i = 0; i < MAX_REPARAMETERISATIONS; i++) {
                params = reparameterise(points, first, curve, params);
                curve = generateBezier(points, first, last, params, startTangent, endTangent);
                maxError = getMaxErrorSquared(points, first, last, curve, params, splitPoint);
                if (maxError < toleranceSquared) {
                    curves.add(curve);
                    return;
                }
            }
        }

        int split = splitPoint[0];
        Coord2D centreTangent = getCentreTangent(points, split);
        fitCubic(points, first, split, startTangent, centreTangent, toleranceSquared, curves);
        fitCubic(
                points, split, last, centreTangent.scale(-1), endTangent, toleranceSquared, curves);
    }

    /** Least-squares fit of the two control point distances along the given tangents. */
    private static CubicBezier generateBezier(
            List<Coord2D> points,
            int first,
            int last,
            float[] params,
            Coord2D startTangent,
            Coord2D endTangent) {
        Coord2D startPoint = points.get(first);
        Coord2D endPoint = points.get(last);

        double c00 = 0, c01 = 0, c11 = 0, x0 = 0, x1 = 0;
        for (int i = 0; i < params.length; i++) {
            float t = params[i];
            float mt = 1 - t;
            float b0 = mt * mt * mt;
            float b1 = 3 * mt * mt * t;
            float b2 = 3 * mt * t * t;
            float b3 = t * t * t;

            Coord2D a0 = startTangent.scale(b1);
            Coord2D a1 = endTangent.scale(b2);
            c00 += dot(a0, a0);
            c01 += dot(a0, a1);
            c11 += dot(a1, a1);

            Coord2D onChord = startPoint.scale(b0 + b1).plus(endPoint.scale(b2 + b3));
            Coord2D residual = points.get(first + i).minus(onChord);
            x0 += dot(a0, residual);
            x1 += dot(a1, residual);
        }

        double determinant = c00 * c11 - c01 * c01;
        double startAlpha = determinant == 0 ? 0 : (x0 * c11 - x1 * c01) / determinant;
        double endAlpha = determinant == 0 ? 0 : (c00 * x1 - c01 * x0) / determinant;

        // a degenerate or backwards solution: fall back to control points a third of the way
        float chord = distance(startPoint, endPoint);
        double epsilon = 1e-6 * chord;
        if (startAlpha < epsilon || endAlpha < epsilon) {
            startAlpha = chord / 3;
            endAlpha = chord / 3;
        }

        return new CubicBezier(
                startPoint,
                startPoint.plus(startTangent.scale((float) startAlpha)),
                endPoint.plus(endTangent.scale((float) endAlpha)),
                endPoint);
    }

    private static float[] reparameterise(
            List<Coord2D> points, int first, CubicBezier curve, float[] params) {
        float[] updated = new float[params.length];
        for (int i = 0; i < params.length; i++) {
            updated[i] = newtonRaphson(curve, points.get(first + i), params[i]);
        }
        return updated;
    }

    /** One Newton-Raphson step towards the parameter of the curve point nearest to point. */
    private static float newtonRaphson(CubicBezier curve, Coord2D point, float t) {
        Coord2D q = curve.pointAt(t);

        // first and second derivatives
        Coord2D d1a = curve.control1.minus(curve.start).scale(3);
        Coord2D d1b = curve.control2.minus(curve.control1).scale(3);
        Coord2D d1c = curve.end.minus(curve.control2).scale(3);
        float mt = 1 - t;
        Coord2D qPrime = d1a.scale(mt * mt).plus(d1b.scale(2 * mt * t)).plus(d1c.scale(t * t));
        Coord2D d2a = d1b.minus(d1a).scale(2);
        Coord2D d2b = d1c.minus(d1b).scale(2);
        Coord2D qPrimePrime = d2a.scale(mt).plus(d2b.scale(t));

        Coord2D offset = q.minus(point);
        float numerator = dot(offset, qPrime);
        float denominator = dot(qPrime, qPrime) + dot(offset, qPrimePrime);
        if (denominator == 0) {
            return t;
        }
        return t - numerator / denominator;
    }

    private static float getMaxErrorSquared(
            List<Coord2D> points,
            int first,
            int last,
            CubicBezier curve,
            float[] params,
            int[] splitPoint) {
        splitPoint[0] = (first + last) / 2;
        float maxError = 0;
        for (int i = first + 1; i < last; i++) {
            Coord2D offset = curve.pointAt(params[i - first]).minus(points.get(i));
            float error = dot(offset, offset);
            if (error >= maxError) {
                maxError = error;
                splitPoint[0] = i;
            }
        }
        return maxError;
    }

    private static float[] chordLengthParameterise(List<Coord2D> points, int first, int last) {
        float[] params = new float[last - first + 1];
        for (int i = first + 1; i <= last; i++) {
            params[i - first] = params[i - first - 1] + distance(points.get(i), points.get(i - 1));
        }
        float total = params[params.length - 1];
        for (int i = 1; i < params.length; i++) {
            params[i] /= total;
        }
        return params;
    }

    /** The tangent at an interior point, pointing back along the line. */
    private static Coord2D getCentreTangent(List<Coord2D> points, int centre) {
        Coord2D back = points.get(centre - 1).minus(points.get(centre)).normalise();
        Coord2D forward = points.get(centre).minus(points.get(centre + 1)).normalise();
        Coord2D tangent = back.plus(forward).scale(0.5f).normalise();
        // the line doubles back on itself here, so there's no average direction to take
        return tangent.mag() > 0 ? tangent : back;
    }

    private static boolean isCorner(List<Coord2D> points, int i) {
        return isCorner(points.get(i - 1), points.get(i), points.get(i + 1));
    }

    private static boolean isCorner(Coord2D previous, Coord2D point, Coord2D next) {
        Coord2D in = point.minus(previous).normalise();
        Coord2D out = next.minus(point).normalise();
        return dot(in, out) < Math.cos(Math.toRadians(CORNER_ANGLE_DEGREES));
    }

    private static List<Coord2D> removeRepeats(List<Coord2D> points) {
        List<Coord2D> distinct = new ArrayList<>();
        for (Coord2D point : points) {
            if (distinct.isEmpty() || !last(distinct).equals(point)) {
                distinct.add(point);
            }
        }
        return distinct;
    }

    private static List<Coord2D> fillIn(List<Coord2D> points, float spacing) {
        List<Coord2D> filled = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Coord2D point = points.get(i);
            if (i > 0) {
                Coord2D previous = points.get(i - 1);
                int steps = (int) Math.ceil(distance(previous, point) / spacing);
                for (int step = 1; step < steps; step++) {
                    filled.add(previous.plus(point.minus(previous).scale((float) step / steps)));
                }
            }
            filled.add(point);
        }
        return filled;
    }

    private static Coord2D last(List<Coord2D> points) {
        return points.get(points.size() - 1);
    }

    private static float dot(Coord2D a, Coord2D b) {
        return a.x * b.x + a.y * b.y;
    }

    private static float distance(Coord2D a, Coord2D b) {
        return a.minus(b).mag();
    }
}
