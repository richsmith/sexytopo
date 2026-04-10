package org.hwyl.sexytopo.control.io.share;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.io.basic.MetadataTranslater;
import org.hwyl.sexytopo.control.io.basic.SketchJsonTranslater;
import org.hwyl.sexytopo.control.io.basic.SurveyJsonTranslater;
import org.hwyl.sexytopo.model.survey.Survey;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SurveyZipSharer implements Shareable {

    static final String FILE_PROVIDER_AUTHORITY = "org.hwyl.sexytopo.fileprovider";
    private static final String SHARE_SUBDIR = "share";

    @Override
    public String getShareTypeName(Context context) {
        return context.getString(R.string.share_type_survey_zip);
    }

    @Override
    public String getMimeType() { return "application/zip"; }

    @Override
    public Uri buildShareUri(Context context, Survey survey) throws Exception {
        Map<String, String> entries = buildZipEntries(context, survey);
        File zipFile = writeZipToCache(context, survey.getName(), entries);
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, zipFile);
    }

    private Map<String, String> buildZipEntries(Context context, Survey survey) throws Exception {
        String name = survey.getName();
        String versionName = SexyTopoActivity.getVersionName(context);
        int versionCode = SexyTopoActivity.getVersionCode(context);

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(SurveyFile.withExtension(name, SexyTopoConstants.DATA_EXTENSION),
                    SurveyJsonTranslater.toText(survey, versionName, versionCode));
        entries.put(SurveyFile.withExtension(name, SexyTopoConstants.METADATA_EXTENSION),
                    MetadataTranslater.translate(survey, versionName, versionCode));
        entries.put(SurveyFile.withExtension(name, SexyTopoConstants.PLAN_SKETCH_EXTENSION),
                    SketchJsonTranslater.translate(
                            survey.getPlanSketch(), survey, versionName, versionCode));
        entries.put(SurveyFile.withExtension(name, SexyTopoConstants.EXT_ELEVATION_SKETCH_EXTENSION),
                    SketchJsonTranslater.translate(
                            survey.getElevationSketch(), survey, versionName, versionCode));
        return entries;
    }

    private File writeZipToCache(Context context, String surveyName,
            Map<String, String> entries) throws IOException {
        File shareDir = new File(context.getCacheDir(), SHARE_SUBDIR);
        shareDir.mkdirs();
        File zipFile = new File(shareDir, surveyName + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes("UTF-8"));
                zos.closeEntry();
            }
        }
        return zipFile;
    }
}
