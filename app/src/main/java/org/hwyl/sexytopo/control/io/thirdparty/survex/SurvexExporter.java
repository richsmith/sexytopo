package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.List;


public class SurvexExporter extends SingleFileExporter {

    protected static GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    public String getContent(Survey survey) {

        StringBuilder builder = new StringBuilder();

        List<GraphToListTranslator.SurveyListEntry> list =
                graphToListTranslator.toListOfSurveyListEntries(survey);

        for (GraphToListTranslator.SurveyListEntry entry : list) {
            formatEntry(builder, entry);
            builder.append("\n");
        }

        return builder.toString();
    }


    protected static void formatEntry(StringBuilder builder,
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



    private static void formatField(StringBuilder builder, Object value) {
        String sValue = value.toString();
        if( value instanceof Number ) {
            sValue = sValue.replace(',', '.');  //use always decimal coma to dot, this is necessary for some localizations
        }
        builder.append(sValue);
        builder.append("\t");
    }

    private static void formatComment(StringBuilder builder, String comment) {
        String formatted = comment.replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n");
        builder.append(formatted);
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


    @Override
    public String getFileExtension() {
        return "svx";
    }


    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_survex);
    }


    @Override
    public String getExportDirectoryName() {
        return "survex";
    }

}
