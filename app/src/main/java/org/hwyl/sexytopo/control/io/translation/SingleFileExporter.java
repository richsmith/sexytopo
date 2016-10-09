package org.hwyl.sexytopo.control.io.translation;

import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;


public abstract class SingleFileExporter extends Exporter {

    public void export(Survey survey) throws IOException {
        String content = getContent(survey);
        String filename = survey.getName() + "." + getFileExtension();
        saveToExportDirectory(survey, filename, content);
    }

    public abstract String getContent(Survey survey);

    public abstract String getFileExtension();

}
