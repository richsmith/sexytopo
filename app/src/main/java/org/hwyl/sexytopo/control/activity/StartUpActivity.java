package org.hwyl.sexytopo.control.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.NewStationNotificationService;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.survey.Survey;


public class StartUpActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);
    }


    @Override
    protected void onStart() {

        super.onStart();

        hasStarted = true;
        Log.load(Log.LogType.SYSTEM);
        Log.i("--------------------");
        Log.i(R.string.log_starting_up);

        try {
            Intent serviceIntent = new Intent(this, NewStationNotificationService.class);
            startService(serviceIntent);

        } catch (Exception exception) {
            // Not mission-critical, so carry on if this doesn't work
            // (some users seem to have had problems according to the log)
            Log.e(R.string.log_error_no_station_notification_intent);
            Log.e(exception);
        }

        initialiseSurvey();

        Intent intent = new Intent(this, DeviceActivity.class);
        startActivity(intent);
    }


    protected void initialiseSurvey() {
        try {
            DocumentFile activeSurveyDirectory = tryToFindActiveSurveyDirectory();

            if (activeSurveyDirectory == null) {
                startNewSurvey();

            } else {
                if (shouldWeRestoreAutosave(activeSurveyDirectory)) {
                    restoreAutosave(activeSurveyDirectory);
                } else {
                    loadSurvey(activeSurveyDirectory);
                }
                loadSurvey(activeSurveyDirectory);
            }

        } catch (Exception exception) {
            Log.e(R.string.file_error_initialising_survey, exception.getMessage());

        } finally {
            if (getSurvey() == null) { // should be impossible
                startNewSurvey();
            }
        }

    }

    public DocumentFile tryToFindActiveSurveyDirectory() {

        Uri activeSurveyUri = GeneralPreferences.getActiveSurveyUri();

        if (activeSurveyUri == null) {
            Log.i(R.string.file_active_survey_not_present);
            return null;
        }

        Log.d(R.string.file_active_survey_uri, activeSurveyUri.toString());

        DocumentFile surveyDirectory = DocumentFile.fromTreeUri(this, activeSurveyUri);

        if (surveyDirectory == null) {
            Log.e(R.string.file_load_survey_error);
            return null;
        }

        if (!IoUtils.doesDirectoryExist(this, activeSurveyUri)) {
            Log.e(R.string.file_load_survey_error);
            return null;

        }

        Log.i(R.string.file_active_survey, surveyDirectory.getName());
        return surveyDirectory;
    }

    private boolean shouldWeRestoreAutosave(DocumentFile surveyDirectory) {

        Survey protoSurvey = new Survey();
        protoSurvey.setDirectory(surveyDirectory);

        boolean shouldRestoreAutosave = false;

        for (SurveyFile.SurveyFileType fileType : SurveyFile.ALL_DATA_TYPES) {
            DocumentFile surveyFile = fileType.get(protoSurvey).getDocumentFile(this);
            DocumentFile autosaveFile = fileType.AUTOSAVE.get(protoSurvey).getDocumentFile(this);

            if (autosaveFile == null) {
                continue;
            }

            if (autosaveFile.lastModified() > surveyFile.lastModified()) {
                shouldRestoreAutosave = true;
                break;
            }
        }

        return shouldRestoreAutosave;
    }

}
