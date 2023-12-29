package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.control.io.SurveyDirectory;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;


@SuppressWarnings("UnnecessaryLocalVariable")
public abstract class Exporter {

    protected Survey survey;

    public void export(Context context, Survey survey) throws IOException {
        this.survey = survey;
        run(context, survey);
    }

    public abstract void run(Context context, Survey survey) throws IOException;

    public abstract String getExportTypeName(Context context);

    public SurveyDirectory getParentExportDirectory() {
        SurveyDirectory exportDirectory = SurveyDirectory.EXPORT.get(survey);
        return exportDirectory;
    }

    public SurveyDirectory getExportDirectory(SurveyDirectory parentDirectory) {
        String directoryName = getExportDirectoryName();
        SurveyDirectory.SurveyDirectoryType directoryType =
                new SurveyDirectory.SurveyDirectoryType(directoryName);
        return directoryType.get(parentDirectory);
    }

    protected abstract String getExportDirectoryName();

    protected SurveyFile getOutputFile(SurveyFile.SurveyFileType surveyFileType) {
        SurveyDirectory parent = getParentExportDirectory();
        SurveyDirectory directory = getExportDirectory(parent);
        SurveyFile surveyFile = surveyFileType.get(directory);
        return surveyFile;
    }

}
