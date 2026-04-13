package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;
import java.io.IOException;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.model.survey.Survey;

public abstract class SingleFileExporter extends Exporter {

    public void run(Context context, Survey survey) throws IOException {
        String content = getContent(survey);
        SurveyFile.SurveyFileType fileType =
                new SurveyFile.SurveyFileType(getFileExtension(), getMimeType());
        SurveyFile surveyFile = getOutputFile(fileType);
        surveyFile.save(context, content);
    }

    public abstract String getContent(Survey survey);

    public abstract String getFileExtension();
}
