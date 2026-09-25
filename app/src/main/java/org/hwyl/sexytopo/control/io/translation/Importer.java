package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.hwyl.sexytopo.model.survey.Survey;

public abstract class Importer {

    public abstract Survey toSurvey(Context context, DocumentFile file) throws Exception;

    public abstract boolean canHandleFile(DocumentFile file);

    /**
     * Imports the given file or folder, reporting the result through the callback. Importers that
     * may need to ask the user something (e.g. which of several files in a folder to use) override
     * this and use the chooser; the default just delegates to toSurvey.
     */
    public void importSurvey(
            Context context, DocumentFile file, ImportChooser chooser, ImportCallback callback) {
        try {
            callback.onImported(toSurvey(context, file));
        } catch (Exception exception) {
            callback.onImportFailed(exception);
        }
    }

    /** A short human-readable name for the format, shown if the user has to pick a format. */
    public String getName() {
        return getClass().getSimpleName().replace("Importer", "");
    }

    public String getDefaultName(DocumentFile file) {
        return FilenameUtils.removeExtension(file.getName());
    }

    /** Returns the files directly inside the directory whose names end with the extension. */
    public static List<DocumentFile> findFiles(DocumentFile directory, String extension) {
        String suffix = "." + extension;
        return Arrays.stream(directory.listFiles())
                .filter(DocumentFile::isFile)
                .filter(file -> file.getName() != null && file.getName().endsWith(suffix))
                .collect(Collectors.toList());
    }

    public static List<String> getNames(List<DocumentFile> files) {
        return files.stream().map(DocumentFile::getName).collect(Collectors.toList());
    }
}
