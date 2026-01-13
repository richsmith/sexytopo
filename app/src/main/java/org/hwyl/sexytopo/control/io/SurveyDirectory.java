package org.hwyl.sexytopo.control.io;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.io.translation.AbstractSurveyFile;
import org.hwyl.sexytopo.model.survey.Survey;

@SuppressWarnings("UnnecessaryLocalVariable")
public class SurveyDirectory extends AbstractSurveyFile {

    public static class SurveyDirectoryType {
        private final String name;

        public SurveyDirectoryType(String name) {
            this.name = name;
        }

        public String getFilename() {
            return name;
        }

        public SurveyDirectory get(Survey survey) {
            if (this == TOP) {
                return new SurveyDirectory(survey, null,this);
            } else {
                return new SurveyDirectory(survey, this);
            }
        }

        public SurveyDirectory get(SurveyDirectory directory) {
            return new SurveyDirectory(directory.survey, directory, this);
        }
    }

    public static final SurveyDirectoryType TOP =
            new SurveyDirectoryType(".");
    public static final SurveyDirectoryType IMPORT_SOURCE =
            new SurveyDirectoryType("Import Source");
    public static final SurveyDirectoryType EXPORT =
            new SurveyDirectoryType("Exported");

    private final SurveyDirectoryType surveyDirectoryType;


    private SurveyDirectory(Survey survey, SurveyDirectoryType surveyDirectoryType) {
        this(survey, TOP.get(survey), surveyDirectoryType);
    }

    private SurveyDirectory(
            Survey survey, SurveyDirectory parent, SurveyDirectoryType surveyDirectoryType) {
        this.survey = survey;
        this.parent = parent;
        this.surveyDirectoryType = surveyDirectoryType;
    }


    public DocumentFile getTopDocumentFile(Context context) {
        Uri uri = survey.getUri();
        DocumentFile directory = DocumentFile.fromTreeUri(context, uri);
        return directory;
    }

    public DocumentFile getDocumentFile(Context context) {
        if (this.surveyDirectoryType == TOP) {
            return getTopDocumentFile(context);
        }
        DocumentFile directory = parent.getDocumentFile(context);
        DocumentFile documentFile = directory.findFile(surveyDirectoryType.getFilename());
        return documentFile;
    }

    public DocumentFile createDocumentFile(Context context) {
        DocumentFile directory = parent.getDocumentFile(context);
        String filename = surveyDirectoryType.getFilename();
        DocumentFile documentFile = directory.createDirectory(filename);
        return documentFile;
    }

    public DocumentFile getOrCreateDocumentFile(Context context) {
        DocumentFile documentFile = getDocumentFile(context);
        if (documentFile == null) {
            documentFile = createDocumentFile(context);
        }
        return documentFile;
    }

    public void ensureExists(Context context) {
        if (this.surveyDirectoryType == TOP) {
            return; // user has to create
        }

        if (!parent.exists(context)) {
            parent.ensureExists(context);
        }
        getOrCreateDocumentFile(context);
    }

}
