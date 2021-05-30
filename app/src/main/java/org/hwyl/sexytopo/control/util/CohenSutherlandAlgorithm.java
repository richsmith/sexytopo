package org.hwyl.sexytopo.control.util;

/*
 * (Adapted from)
 * Devin Carraway <ssu-cs360@devin.com>
 * CS360, Final Project
 *
 * Released under terms of the GNU GPL. See
 * http://www.gnu.org/copyleft/gpl.html for details.
 *
 */

/* CohenSutherlandAlgorithm: implements the Cohen-Sutherland line clipping
 *   algorithm, excluding vertex replacement.
 *
 *   Because speed is important in clipping, static methods are the
 *   only ones actually used here.
 */

import org.hwyl.sexytopo.model.graph.Coord2D;

public class CohenSutherlandAlgorithm
{
    /* Convention: p1,p2 are the vertices of the clipped edge.
     * cp1, cp2 are the vertices giving the clipping region
     */


    /* Determines if line(p1,p2) lies entirely inside rect(cp1,cp2) */
    public static boolean whollyInside(Coord2D p1, Coord2D p2, Coord2D cp1, Coord2D cp2)
    {
        return ((bitcode(cp1, cp2, p1) | bitcode(cp1, cp2, p2)) == 0);
    }

    /* Determines if line(p1,p2) is entirely outside rect(cp1,cp2) */
    public static boolean whollyOutside(Coord2D p1, Coord2D p2, Coord2D cp1,
                                        Coord2D cp2)
    {
        int p1code, p2code, icode;
        p1code = bitcode(cp1, cp2, p1);
        p2code = bitcode(cp1, cp2, p2);
        icode = p1code & p2code;
        return (p1code != 0 && p2code != 0 && icode != 0);
    }

    /* Determines if line(p1,p2) intersects in any way rect(cp1,cp2).
     * This is actually the first half of the Cohen-Sutherland algorithm;
     * the second half would be adjusting the edges to fit inside the
     * clip rectangle, which isn't germane to our purposes.
     *
     * The general algorithm: each vertex has a 4-bit code computed, which
     * indicates its position with respect to the clipping rectangle.  The
     * bitwise-AND of each vertex of an edge indicates whether the line
     * lies entirely inside (0 & 0 == 0), entirely outside (nonzero &
     * nonzero2) == nonzero3), or if it crosses the clipping rectangle (any
     * other possibility).
     */
    public static boolean clips(Coord2D p1, Coord2D p2, Coord2D cp1, Coord2D cp2)
    {
        int p1code, p2code, icode;
        boolean r;

        p1code = bitcode(cp1, cp2, p1);
        p2code = bitcode(cp1, cp2, p2);
        icode = p1code & p2code;

        if (p1code == 0 && p2code == 0)
            r = false;
        else if (p1code != 0 && p2code != 0 && icode != 0)
            r = false;
        else
            r = true;
        return r;
    }

    /* Computes the 4-bit code for a point p with respect
     * to line(p1,p2)
     */
    public static int bitcode(Coord2D p1, Coord2D p2, Coord2D p)
    {
        return (above(p1, p2, p) | below(p1, p2, p) |
                left(p1, p2, p) | right(p1, p2, p));
    }

    /* Gives the 2**3 bit (point above line(p1,p2)) */
    public static int above(Coord2D p1, Coord2D p2, Coord2D p)
    {
        if (p.y < Math.min(p1.y, p2.y))
            return 0x8;
        return 0;
    }

    /* Gives the 2**2 bit (point below line(p1,p2)) */
    public static int below(Coord2D p1, Coord2D p2, Coord2D p)
    {
        if (p.y > Math.max(p1.y, p2.y))
        {
            return 0x4;
        }
        return 0;
    }

    /* Gives the 2**1 bit (point right of line(p1,p2)) */
    public static int right(Coord2D p1, Coord2D p2, Coord2D p)
    {
        if (p.x > Math.max(p1.x, p2.x))
            return 0x2;
        return 0;
    }

    /* Gives the 2**0 bit (point left of line(p1,p2)) */
    public static int left(Coord2D p1, Coord2D p2, Coord2D p)
    {
        if (p.x < Math.min(p1.x, p2.x))
            return 0x1;
        return 0;
    }
}
