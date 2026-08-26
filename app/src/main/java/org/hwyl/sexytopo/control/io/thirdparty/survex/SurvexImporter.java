package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SexyTopoVersion;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

public class SurvexImporter extends Importer {

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

    @Override
    public boolean canHandleFile(DocumentFile file) {
        return file.getName().endsWith(".svx");
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
