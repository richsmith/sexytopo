package org.hwyl.sexytopo.control.io.thirdparty.compass;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


/**
 * Survey exporter which targets the Windows-only software Compass by Fountainware / Larry Fish.
 *
 * Created by driggs on 1/16/16.
 */
public class CompassExporter extends SingleFileExporter {

    private static DateFormat dateFormat = new SimpleDateFormat("MM dd yyyy");
    private static GraphToListTranslator graphToListTranslator = new GraphToListTranslator();
    private Station currentFrom;
    private int splayCount;

    final public static double METERS_TO_FEET = 3.28084;

    /**
     * Export a SexyTopo Survey as a Compass .DAT file.
     * @param survey
     * @return
     */
    public String getContent(Survey survey) {
        List<GraphToListTranslator.SurveyListEntry> data = graphToListTranslator.toListOfSurveyListEntries(survey);
        String surveyDate = dateFormat.format(Calendar.getInstance().getTime());

        StringBuilder sb = new StringBuilder(1024);
        sb.append("SexyTopo Export\r\n");
        sb.append(String.format("SURVEY NAME: %s\r\n", survey.getName()));
        sb.append(String.format("SURVEY DATE: %s\tCOMMENT: \r\n", surveyDate));
        sb.append("SURVEY TEAM:\r\n\r\n");
        sb.append("DECLINATION: 0.00\tFORMAT: DMMDLRUDLADNF\tCORRECTIONS: 0.00 0.00 0.00\r\n");
        sb.append("\r\n");
        sb.append("FROM\tTO\tLENGTH\tBEARING\tINC\tLEFT\tUP\tDOWN\tRIGHT\tFLAGS\tCOMMENTS\r\n");
        sb.append("\r\n");

        for (GraphToListTranslator.SurveyListEntry entry : data) {
            Leg leg = entry.getLeg();
            Station from = entry.getFrom();
            String to = leg.hasDestination() ? leg.getDestination().toString() : this.splayStationFrom(from);
            double dist = leg.getDistance() * METERS_TO_FEET;  // all Compass lengths are decimal feet!
            double azm = leg.getAzimuth();
            double inc = leg.getInclination();

            sb.append(String.format("%s\t%s\t%.2f\t%.2f\t%.2f\t", from, to, dist, azm, inc));
            sb.append("-9.99\t-9.99\t-9.99\t-9.99\t");  // LUDR, must be in that order
            if (!leg.hasDestination()) {
                sb.append("#|L#");  // exclude splay shots from cave length calculations
            }
            sb.append("\t\t\r\n");  // empty comments field
        }
        sb.append('\f');  // ASCII formfeed denotes end of survey

        return sb.toString();
    }


    @Override
    public String getFileExtension() {
        return "dat";
    }


    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_compass);
    }


    @Override
    public String getExportDirectoryName() {
        return "compass";
    }


    /**
     * Produce a unique TO station name for a splay shot (Compass doesn't allow anonymous stations).
     * @param from
     * @return A station label of, for example, `A53ss003` for the third splay off station A53
     */
    private String splayStationFrom(Station from) {
        if (!from.equals(this.currentFrom)) {
            this.currentFrom = from;
            this.splayCount = 0;
        }
        return String.format("%sss%03d", from, this.splayCount++);
    }

}
