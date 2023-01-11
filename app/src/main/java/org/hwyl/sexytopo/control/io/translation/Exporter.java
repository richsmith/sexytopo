package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;

import androidx.documentfile.provider.DocumentFile;


@SuppressWarnings("UnnecessaryLocalVariable")
public abstract class Exporter {

    public abstract void export(Context context, Survey survey) throws IOException;

    public abstract String getExportTypeName(Context context);

    public abstract String getExportDirectoryName();

    protected DocumentFile getOrCreateExportDirectory(Context context, Survey survey) {
        DocumentFile baseExportDirectory = IoUtils.getExportDirectory(context, survey);
        String exportDirectoryName = getExportDirectoryName();
        DocumentFile exportDirectory =
                IoUtils.getOrCreateDirectory(baseExportDirectory, exportDirectoryName);
        return exportDirectory;
    }


    protected void saveToExportDirectory(
            Context context, Survey survey,
            String mimeType, String filename,
            String content)
            throws IOException {

        DocumentFile directory = getOrCreateExportDirectory(context, survey);
        DocumentFile file = IoUtils.getOrCreateFile(directory, mimeType, filename);
        Saver.saveFile(context, file, content);
    }

}
