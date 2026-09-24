package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SexyTopoVersion;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.translation.ImportCallback;
import org.hwyl.sexytopo.control.io.translation.ImportChooser;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

public class SurvexImporter extends Importer {

    @Override
    public boolean canHandleFile(DocumentFile file) {
        if (file.isDirectory()) {
            return !findFiles(file, SurvexConstants.SVX_EXTENSION).isEmpty();
        }
        return isSvx(file);
    }

    /**
     * Importing a folder lets us pick up the .espec file (extended elevation directions) that sits
     * alongside the .svx. A lone .svx file can't see its siblings, so every leg goes right.
     */
    @Override
    public void importSurvey(
            Context context, DocumentFile file, ImportChooser chooser, ImportCallback callback) {
        if (!file.isDirectory()) {
            super.importSurvey(context, file, chooser, callback);
            return;
        }

        List<DocumentFile> svxFiles = findFiles(file, SurvexConstants.SVX_EXTENSION);
        if (svxFiles.size() == 1) {
            importFromDirectory(context, file, svxFiles.get(0), chooser, callback);
        } else {
            chooser.choose(
                    R.string.import_choose_svx_file,
                    getNames(svxFiles),
                    index ->
                            importFromDirectory(
                                    context, file, svxFiles.get(index), chooser, callback));
        }
    }

    private void importFromDirectory(
            Context context,
            DocumentFile directory,
            DocumentFile svxFile,
            ImportChooser chooser,
            ImportCallback callback) {

        List<DocumentFile> especFiles = findFiles(directory, SurvexConstants.ESPEC_EXTENSION);
        DocumentFile matchingEspec = findMatchingEspec(svxFile, especFiles);

        if (matchingEspec != null || especFiles.isEmpty()) {
            importWithEspec(context, svxFile, matchingEspec, callback);
        } else {
            // There are .espec files but none named after the .svx; let the user pick or skip
            List<String> options = new ArrayList<>(getNames(especFiles));
            options.add(context.getString(R.string.import_espec_none));
            chooser.choose(
                    R.string.import_choose_espec,
                    options,
                    index -> {
                        DocumentFile espec =
                                index < especFiles.size() ? especFiles.get(index) : null;
                        importWithEspec(context, svxFile, espec, callback);
                    });
        }
    }

    private void importWithEspec(
            Context context,
            DocumentFile svxFile,
            DocumentFile especFile,
            ImportCallback callback) {
        try {
            Survey survey = toSurvey(context, svxFile);
            if (especFile != null) {
                EspecParser.applyEspec(context, especFile, survey);
            }
            callback.onImported(survey);
        } catch (Exception exception) {
            callback.onImportFailed(exception);
        }
    }

    /** Returns the .espec with the same base name as the .svx, or null if there isn't one. */
    static DocumentFile findMatchingEspec(DocumentFile svxFile, List<DocumentFile> especFiles) {
        String svxBaseName = FilenameUtils.removeExtension(svxFile.getName());
        return especFiles.stream()
                .filter(espec -> FilenameUtils.removeExtension(espec.getName()).equals(svxBaseName))
                .findFirst()
                .orElse(null);
    }

    private static boolean isSvx(DocumentFile file) {
        return file.getName() != null
                && file.getName().endsWith("." + SurvexConstants.SVX_EXTENSION);
    }

    @Override
    public Survey toSurvey(Context context, DocumentFile file) throws Exception {
        Survey survey = new Survey();
        String text = IoUtils.slurpFile(context, file);

        // Determine import mode based on the SexyTopo version that wrote the file.
        // Files with no version header (third-party) or written by 1.11.3+ use the new
        // leg-comment path. Only files positively identified as 1.11.2 or earlier use the
        // legacy station-comment path.
        SexyTopoVersion version = SexyTopoVersion.extractFromText(text);
        boolean useLegComments =
                version == null || version.isAfter(SexyTopoVersion.LEG_COMMENTS_VERSION_CUTOFF);

        // Parse passage data first to extract station comments
        Map<String, String> passageComments =
                SurvexTherionImporter.parsePassageData(text, SurveyFormat.SURVEX);

        // Parse centreline data from the normal data block only.
        // This avoids trying to parse passage rows (e.g. "1 - - - - comment") as shots.
        String centrelineText = extractNormalDataBlock(text, SurveyFormat.SURVEX);
        SurvexTherionImporter.parseCentreline(centrelineText, survey, useLegComments);

        // Merge passage comments with station comments
        SurvexTherionImporter.mergePassageComments(survey, passageComments);

        // Parse trip metadata (date, instrument, team, etc.)
        Trip trip = SurvexTherionImporter.parseMetadata(text, SurveyFormat.SURVEX);
        if (trip != null) {
            survey.setTrip(trip);
        }

        return survey;
    }

    private static String extractNormalDataBlock(String text, SurveyFormat format) {
        String[] lines = text.split("\n");
        List<String> normalDataLines = new ArrayList<>();

        String dataCommandPrefix = format.getCommandChar() + "data ";
        String dataNormalPrefix = format.getCommandChar() + "data normal";
        boolean inNormalDataBlock = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith(dataCommandPrefix)) {
                inNormalDataBlock = trimmed.startsWith(dataNormalPrefix);
                continue;
            }

            if (inNormalDataBlock) {
                normalDataLines.add(line);
            }
        }

        return TextTools.join("\n", normalDataLines);
    }
}
