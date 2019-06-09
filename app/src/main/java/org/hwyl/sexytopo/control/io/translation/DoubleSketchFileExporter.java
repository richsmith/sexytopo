package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;


public abstract class DoubleSketchFileExporter extends Exporter {

    public void export(Context context, Survey survey) throws IOException {
        String planContent = getContent(survey.getPlanSketch());
        saveSketchFile(context, survey, planContent, SexyTopo.PLAN_SUFFIX);
        String eeContent = getContent(survey.getElevationSketch());
        saveSketchFile(context, survey, eeContent, SexyTopo.EE_SUFFIX);
    }

    protected void saveSketchFile(Context context, Survey survey, String content, String suffix)
            throws IOException {
        String filename = survey.getName() + " " + suffix + "." + getFileExtension();
        saveToExportDirectory(context, survey, filename, content);
    }

    public abstract String getContent(Sketch sketch) throws IOException;

    public abstract String getFileExtension();

}
