package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * Created by rls on 16/02/15.
 */
public class Space2DUtils {


    public static double getDistanceFromLine(Coord2D point, Coord2D lineStart, Coord2D lineEnd) {

        // Adapted from a post on StackExchange by Joshua
        // http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment

        double x = point.getX();
        double y = point.getY();
        double x1 = lineStart.getX();
        double y1 = lineStart.getY();
        double x2 = lineEnd.getX();
        double y2 = lineEnd.getY();

        double a = x - x1;
        double b = y - y1;
        double c = x2 - x1;
        double d = y2 - y1;

        double dot = a * c + b * d;
        double lenSq = c * c + d * d;
        double param = -1;

        if (lenSq != 0) {
            param = dot / lenSq;
        }

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * c;
            yy = y1 + param * d;
        }

        double dx = x - xx;
        double dy = y - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }


    public static double getDistance(Coord2D a, Coord2D b) {
        return Math.sqrt(Math.pow((a.getX() - b.getX()), 2) + Math.pow((a.getY() - b.getY()), 2));
    }

    public static double adjustAngle(double angle, double delta) {
        double newAngle = angle + delta;
        while (newAngle < 0) {
            newAngle += 360;
        }
         return newAngle % 360;
    }
}
