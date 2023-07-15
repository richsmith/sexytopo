package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.io.SurveyDirectory;
import org.hwyl.sexytopo.model.survey.Survey;

public abstract class AbstractSurveyFile {

    protected Survey survey;
    protected SurveyDirectory parent;

    public Survey getSurvey() {
        return survey;
    }

    public abstract DocumentFile getDocumentFile(Context context);

    public boolean exists(Context context) {
        DocumentFile documentFile = getDocumentFile(context);
        return documentFile != null && documentFile.exists();
    }

}
