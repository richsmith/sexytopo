package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.apache.commons.io.FilenameUtils;
import org.hwyl.sexytopo.model.survey.Survey;

import androidx.documentfile.provider.DocumentFile;


public abstract class Importer {

    public abstract Survey toSurvey(Context context, DocumentFile file) throws Exception;

    public abstract boolean canHandleFile(DocumentFile file);

    public String getDefaultName(DocumentFile file) {
        return FilenameUtils.removeExtension(file.getName());
    }
}
