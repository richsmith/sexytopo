package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * Created by rls on 26/07/14.
 */
public class Space3DTransformerForElevation  extends Space3DTransformer {


    public Coord3D transform(Coord3D start, Leg leg) {
        double r = leg.getDistance();

        double phi = 0;

        double theta = leg.getInclination();

        phi = Math.toRadians(phi);
        theta = Math.toRadians(theta);

        double y = r * Math.cos(theta) * Math.cos(phi);
        double x = r * Math.cos(theta) * Math.sin(phi);
        double z = r * Math.sin(theta);

        x += start.getX();
        y += start.getY();
        z += start.getZ();

        return new Coord3D(x, y, z);
    }

}
