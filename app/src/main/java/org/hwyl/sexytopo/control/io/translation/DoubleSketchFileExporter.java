package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;


public abstract class DoubleSketchFileExporter extends Exporter {

    public void run(Context context, Survey survey) throws IOException {
        String planContent = getContent(survey.getPlanSketch());
        saveSketchFile(context, planContent, SexyTopoConstants.PLAN_SUFFIX);
        String eeContent = getContent(survey.getElevationSketch());
        saveSketchFile(context, eeContent, SexyTopoConstants.EE_SUFFIX);
    }

    protected void saveSketchFile(Context context, String content, String suffix)
            throws IOException {
        String extension = getFileExtension();
        SurveyFile.SurveyFileType fileType =
                new SurveyFile.SurveyFileType(suffix + "." + extension);
        SurveyFile outputFile = getOutputFile(fileType);
        outputFile.save(context, content);
    }

    public abstract String getContent(Sketch sketch) throws IOException;

    public abstract String getFileExtension();

}
