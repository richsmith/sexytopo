package org.hwyl.sexytopo.control.io;

import org.hwyl.sexytopo.model.Survey;
import org.hwyl.sexytopo.model.sketch.Sketch;

/**
 * Created by rls on 11/12/14.
 */
public class TherionExporter {



    public static String export(Survey survey) {

        String centerlineText =
            "centreline\n\n" +
            indent(getCentreline(survey)) + "\n\n" +
            "endcentreline";

        String surveyText =
            "survey " + survey.getName() + "\n\n" +
            indent(centerlineText) +
            "\n\nendsurvey";

        return surveyText;
    }

    public static String indent(String text) {
        String indented = "";
        String[] lines = text.split("\n");
        for (String line : lines) {
            indented += "\t" + line + "\n";
        }
        return indented;
    }




    private static String getCentreline(Survey survey) {
        return "data normal from to length compass clino\n\n" +
            SurvexExporter.export(survey);
    }


    public static String exportSketch(Sketch sketch) {
        String text = "";
        for (Sketch.PathDetail pathDetail : sketch.getPathDetails()) {
            //pathDetail.getPath();
        }

        return text;
    }





    /*
    public void exportTherion( BufferedWriter out, String scrap_name, String proj_name )
    {
        try {
            out.write("encoding utf-8");
            out.newLine();
            out.newLine();
            StringWriter sw = new StringWriter();
            PrintWriter pw  = new PrintWriter(sw);
            pw.format("scrap %s -projection %s -scale [0 0 1 0 0.0 0.0 1 0.0 m]", scrap_name, proj_name );
            out.write( sw.getBuffer().toString() );
            out.newLine();
            // out.newLine();
            // for ( DrawingStation st : mStations ) {
            //   out.write( st.toTherion() );
            //   out.newLine();
            // }
            out.newLine();
            float xmin=10000f, xmax=-10000f,
                    ymin=10000f, ymax=-10000f,
                    umin=10000f, umax=-10000f,
                    vmin=10000f, vmax=-10000f;
            for ( DrawingPath p : mCurrentStack ) {
                if ( p.mType == DrawingPath.DRAWING_PATH_POINT ) {
                    DrawingPointPath pp = (DrawingPointPath)p;
                    out.write( pp.toTherion() );
                    out.newLine();
                } else if ( p.mType == DrawingPath.DRAWING_PATH_LINE ) {
                    DrawingLinePath lp = (DrawingLinePath)p;
                    // Log.v( TAG, " saving line " + lp.lineType() );
                    if ( lp.lineType() == DrawingBrushPaths.LINE_WALL ) {
                        ArrayList< LinePoint > pts = lp.getPoints();
                        for ( LinePoint pt : pts ) {
                            if ( pt.mX < xmin ) xmin = pt.mX;
                            if ( pt.mX > xmax ) xmax = pt.mX;
                            if ( pt.mY < ymin ) ymin = pt.mY;
                            if ( pt.mY > ymax ) ymax = pt.mY;
                            float u = pt.mX + pt.mY;
                            float v = pt.mX - pt.mY;
                            if ( u < umin ) umin = u;
                            if ( u > umax ) umax = u;
                            if ( v < vmin ) vmin = v;
                            if ( v > vmax ) vmax = v;
                        }
                    }
                    out.write( lp.toTherion() );
                    out.newLine();
                } else if ( p.mType == DrawingPath.DRAWING_PATH_AREA ) {
                    DrawingAreaPath ap = (DrawingAreaPath)p;
                    // Log.v( TAG, " saving area " + ap.areaType() );
                    out.write( ap.toTherion() );
                    out.newLine();
                }
            }
            out.newLine();
            for ( DrawingStation st : mStations ) {
                // FIXME if station is in the convex hull of the lines
                if ( xmin > st.mX || xmax < st.mX ) continue;
                if ( ymin > st.mY || ymax < st.mY ) continue;
                float u = st.mX + st.mY;
                float v = st.mX - st.mY;
                if ( umin > u || umax < u ) continue;
                if ( vmin > v || vmax < v ) continue;
                out.write( st.toTherion() );
                out.newLine();
            }
            out.newLine();
            out.newLine();
            out.write("endscrap");
            out.newLine();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
*/

}
