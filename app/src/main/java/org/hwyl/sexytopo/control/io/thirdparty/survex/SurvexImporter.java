package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SexyTopoVersion;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.translation.FolderImporter;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

public class SurvexImporter extends FolderImporter {

    @Override
    public List<DocumentFile> getCandidateFiles(DocumentFile directory) {
        List<DocumentFile> candidates = new ArrayList<>();
        for (DocumentFile file : directory.listFiles()) {
            if (file.isFile() && file.getName() != null && file.getName().endsWith(".svx")) {
                candidates.add(file);
            }
        }
        return candidates;
    }

    @Override
    public Survey toSurvey(Context context, DocumentFile svxFile, DocumentFile directory)
            throws Exception {
        Survey survey = new Survey();
        String text = IoUtils.slurpFile(context, svxFile);

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

    /**
     * Finds the matching .espec file for the given .svx file within the directory. Returns the
     * name-matched .espec if one exists, otherwise returns all .espec files found (may be empty).
     */
    public static EspecResolution resolveEspec(DocumentFile svxFile, DocumentFile directory) {
        String svxBaseName = FilenameUtils.removeExtension(svxFile.getName());
        List<DocumentFile> allEspecFiles = new ArrayList<>();

        for (DocumentFile file : directory.listFiles()) {
            if (file.isFile() && file.getName() != null && file.getName().endsWith(".espec")) {
                allEspecFiles.add(file);
            }
        }

        for (DocumentFile especFile : allEspecFiles) {
            String especBaseName = FilenameUtils.removeExtension(especFile.getName());
            if (especBaseName.equals(svxBaseName)) {
                return EspecResolution.matched(especFile);
            }
        }

        return EspecResolution.unmatched(allEspecFiles);
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
