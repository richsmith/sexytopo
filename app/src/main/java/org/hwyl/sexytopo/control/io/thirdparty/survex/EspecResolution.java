package org.hwyl.sexytopo.control.io.thirdparty.survex;

import androidx.documentfile.provider.DocumentFile;
import java.util.Collections;
import java.util.List;

/**
 * The result of resolving a .espec file for a given .svx file. Three outcomes are possible:
 *
 * <ul>
 *   <li>{@link #isMatched()} — a .espec with the same base name was found; use it silently.
 *   <li>{@link #isUnmatched()} with a non-empty {@link #getOtherEspecFiles()} list — no name match
 *       but other .espec files exist; prompt the user to pick one or skip.
 *   <li>{@link #isUnmatched()} with an empty {@link #getOtherEspecFiles()} list — no .espec files
 *       at all; silently default to all RIGHT.
 * </ul>
 */
public class EspecResolution {

    private final DocumentFile matchedFile;
    private final List<DocumentFile> otherEspecFiles;

    private EspecResolution(DocumentFile matchedFile, List<DocumentFile> otherEspecFiles) {
        this.matchedFile = matchedFile;
        this.otherEspecFiles = otherEspecFiles;
    }

    static EspecResolution matched(DocumentFile file) {
        return new EspecResolution(file, Collections.<DocumentFile>emptyList());
    }

    static EspecResolution unmatched(List<DocumentFile> otherFiles) {
        return new EspecResolution(null, Collections.unmodifiableList(otherFiles));
    }

    /** Returns true if a name-matched .espec file was found. */
    public boolean isMatched() {
        return matchedFile != null;
    }

    /** Returns true if no name-matched .espec file was found. */
    public boolean isUnmatched() {
        return matchedFile == null;
    }

    /**
     * Returns the name-matched .espec file. Only valid when {@link #isMatched()} is true; returns
     * null otherwise.
     */
    public DocumentFile getMatchedFile() {
        return matchedFile;
    }

    /**
     * Returns all .espec files found in the directory when there was no name match. Empty when
     * {@link #isMatched()} is true or when no .espec files exist at all.
     */
    public List<DocumentFile> getOtherEspecFiles() {
        return otherEspecFiles;
    }
}
