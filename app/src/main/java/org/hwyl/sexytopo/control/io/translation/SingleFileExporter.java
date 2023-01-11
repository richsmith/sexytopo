package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;


public abstract class SingleFileExporter extends Exporter {

    public void export(Context context, Survey survey) throws IOException {
        String content = getContent(survey);
        String filename = survey.getName() + "." + getFileExtension();
        String mimeType = getMimeType();
        saveToExportDirectory(context, survey, mimeType, filename, content);
    }

    public abstract String getContent(Survey survey);

    public abstract String getFileExtension();

    public String getMimeType() {
        return "text/plain";
    }

}
