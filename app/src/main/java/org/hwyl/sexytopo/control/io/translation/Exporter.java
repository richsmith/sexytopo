package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.SurveyDirectory;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;


@SuppressWarnings("UnnecessaryLocalVariable")
public abstract class Exporter {

    private Survey survey;

    public void export(Context context, Survey survey) throws Exception {
        this.survey = survey;
        run(context, survey);
    }

    public abstract void run(Context context, Survey survey) throws Exception;

    public abstract String getExportTypeName(Context context);

    public String getExportTypeDescription(Context context) {
        String name = getExportTypeName(context);
        if (this instanceof Experimental) {
            String experimental = context.getString(R.string.experimental_warning);
            return TextTools.join(" ", name, "[" + experimental + "]");
        } else {
            return name;
        }
    }

    public SurveyDirectory getParentExportDirectory() {
        String folderName = GeneralPreferences.getExportFolderName();
        if (folderName == null || folderName.trim().isEmpty()) {
            return SurveyDirectory.TOP.get(survey);
        }
        SurveyDirectory.SurveyDirectoryType exportDirectoryType =
                new SurveyDirectory.SurveyDirectoryType(folderName);
        SurveyDirectory exportDirectory = exportDirectoryType.get(survey);
        return exportDirectory;
    }

    public SurveyDirectory getExportDirectory(SurveyDirectory parentDirectory) {
        if (!GeneralPreferences.isExportTypeSubfoldersEnabled()) {
            return parentDirectory;
        }
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


    public String getMimeType() {
        return "text/plain";
    }

}
