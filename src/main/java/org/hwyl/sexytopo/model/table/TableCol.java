package org.hwyl.sexytopo.model.table;

import org.hwyl.sexytopo.R;

/**
 * Created by rls on 04/08/14.
 */
public enum TableCol {

    FROM(R.id.tableRowFrom, "From", "%s"),
    TO(R.id.tableRowTo, "To", "%s"),
    DISTANCE(R.id.tableRowDistance, "Distance", "%.2f"),
    BEARING(R.id.tableRowBearing, "Bearing", "%.2f"),
    INCLINATION(R.id.tableRowInclination, "Inclination", "%.2f");

    final int id;
    final String name;
    final String format;

    private TableCol(int id, String name, String format) {
        this.id = id;
        this.name = name;
        this.format = format;
    }

    public String format(Object object) {
        return String.format(format, object);
    }
}
