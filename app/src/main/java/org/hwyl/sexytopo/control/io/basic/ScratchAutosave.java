package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Autosave for a survey that hasn't been saved anywhere yet, so has no folder of its own for the
 * normal autosave files. Only one such survey is kept, in the app's private storage, and it's
 * cleared once the survey is saved properly or deliberately discarded.
 */
public class ScratchAutosave {

    private static final String DIRECTORY_NAME = "unsaved-survey";
    private static final String FILENAME = "unsaved";

    public static synchronized void save(Context context, Survey survey) throws Exception {
        File directory = getDirectory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create " + directory);
        }

        for (SurveyFile.SurveyFileType type : SurveyFile.ALL_DATA_TYPES) {
            String text = Saver.toText(context, survey, type);
            // write to a temporary file first so being killed mid-write can't truncate the file
            File file = getFile(context, type);
            File temp = new File(directory, file.getName() + ".tmp");
            DocumentFile tempDocument = DocumentFile.fromFile(temp);
            IoUtils.saveToFile(context, tempDocument, text);
            if (!temp.renameTo(file)) {
                throw new IOException("Could not write " + file);
            }
        }
        survey.setAutosaved(true);
    }

    public static synchronized boolean exists(Context context) {
        return getFile(context, SurveyFile.DATA).exists();
    }

    public static synchronized Survey load(Context context) throws Exception {
        Survey survey = new Survey();

        Loader.SurveyTextSource scratchFiles =
                type -> {
                    File file = getFile(context, type);
                    if (!file.exists()) {
                        return null;
                    }
                    DocumentFile document = DocumentFile.fromFile(file);
                    String text = IoUtils.slurpFile(context, document);
                    return text;
                };
        Set<Uri> surveyUrisNotToLoad = new HashSet<>();
        Loader.populate(context, survey, scratchFiles, surveyUrisNotToLoad);

        // it's still not saved anywhere, so the next save should ask where to put it
        survey.setSaved(false);
        return survey;
    }

    public static synchronized void clear(Context context) {
        File[] files = getDirectory(context).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static File getDirectory(Context context) {
        return new File(context.getFilesDir(), DIRECTORY_NAME);
    }

    private static File getFile(Context context, SurveyFile.SurveyFileType type) {
        return new File(
                getDirectory(context), SurveyFile.withExtension(FILENAME, type.getExtension()));
    }
}
