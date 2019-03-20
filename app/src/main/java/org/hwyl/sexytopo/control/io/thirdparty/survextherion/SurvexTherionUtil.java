package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.table.TableCol;


public class SurvexTherionUtil {


    public static void formatEntry(StringBuilder builder,
                                   GraphToListTranslator.SurveyListEntry entry) {

        Station from = entry.getFrom();
        Leg leg = entry.getLeg();
        Station to = leg.getDestination();

        formatField(builder, from.getName());
        formatField(builder, to.getName());
        formatField(builder, TableCol.DISTANCE.format(leg.getDistance()));
        formatField(builder, TableCol.AZIMUTH.format(leg.getAzimuth()));
        formatField(builder, TableCol.INCLINATION.format(leg.getInclination()));

        if (leg.wasPromoted() || to.hasComment()) {
            builder.append("\t; ");
            if (leg.wasPromoted()) {
                builder.append(" ");
                formatPromotedFrom(builder, leg.getPromotedFrom());
            }
            if (to.hasComment()) {
                builder.append(" ");
                formatComment(builder, to.getComment());

            }
        }


    }

    private static void formatPromotedFrom(StringBuilder builder, Leg[] precursors) {
        builder.append("{from: ");
        boolean first = true;
        for (Leg precursor : precursors) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(TableCol.DISTANCE.format(precursor.getDistance()));
            builder.append(" ");
            builder.append(TableCol.AZIMUTH.format(precursor.getAzimuth()));
            builder.append(" ");
            builder.append(TableCol.INCLINATION.format(precursor.getInclination()));
        }
        builder.append("}");
    }



    private static void formatField(StringBuilder builder, Object value) {
        builder.append(value.toString());
        builder.append("\t");
    }

    private static void formatComment(StringBuilder builder, String comment) {
        String formatted = comment.replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n");
        builder.append(formatted);
    }
}
