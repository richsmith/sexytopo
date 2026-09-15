package org.hwyl.sexytopo.control.io.translation;

import androidx.documentfile.provider.DocumentFile;
import java.util.List;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Base class for importers that operate on a directory rather than a single file. Subclasses
 * provide the list of candidate files within the directory, and import a specific chosen file with
 * access to its siblings.
 */
public abstract class FolderImporter extends Importer {

    /**
     * Returns all importable candidate files found in the given directory. The caller uses this
     * list to determine whether to proceed silently (exactly one candidate) or to prompt the user
     * to choose.
     */
    public abstract List<DocumentFile> getCandidateFiles(DocumentFile directory);

    /**
     * Imports the given file, with access to sibling files in the directory (e.g. for companion
     * files such as .espec or .xvi).
     */
    public abstract Survey toSurvey(
            android.content.Context context, DocumentFile file, DocumentFile directory)
            throws Exception;

    /**
     * Default single-file entry point. Delegates to the directory-aware overload using the file's
     * parent directory. Kept for compatibility with the {@link Importer} contract.
     */
    @Override
    public Survey toSurvey(android.content.Context context, DocumentFile file) throws Exception {
        return toSurvey(context, file, file.getParentFile());
    }

    /**
     * Returns true if the given directory contains at least one candidate file for this importer.
     */
    @Override
    public boolean canHandleFile(DocumentFile directory) {
        if (directory == null || !directory.isDirectory()) {
            return false;
        }
        return !getCandidateFiles(directory).isEmpty();
    }
}
