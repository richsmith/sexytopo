package org.hwyl.sexytopo.control.io.translation;

import org.hwyl.sexytopo.model.survey.Survey;

public class RecordingImportCallback implements ImportCallback {

    public Survey survey;
    public Exception exception;

    @Override
    public void onImported(Survey survey) {
        this.survey = survey;
    }

    @Override
    public void onImportFailed(Exception exception) {
        this.exception = exception;
    }
}
