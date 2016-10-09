package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.io.IOException;


public abstract class Exporter {

    public abstract void export(Survey survey) throws IOException;

    public abstract String getExportTypeName(Context context);

    public abstract String getExportDirectoryName();

    private String createExportDirectoryIfRequired(Survey survey) {
        String path = getExportDirectoryPath(survey);
        Util.ensureDirectoriesInPathExist(path);
        return path;
    }

    private String getExportDirectoryPath(Survey survey) {
        return Util.getExportDirectoryPathForSurvey(
                survey.getName()) + File.separator + getExportDirectoryName();
    }

    protected void saveToExportDirectory(Survey survey, String filename, String content)
            throws IOException {
        createExportDirectoryIfRequired(survey);
        String path = getExportDirectoryPath(survey) + File.separator + filename;
        Saver.saveFile(path, content);
    }

}
