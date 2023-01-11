package org.hwyl.sexytopo.control.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.Instrument;
import org.hwyl.sexytopo.comms.missing.NullCommunicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.control.io.translation.ImportManager;
import org.hwyl.sexytopo.control.io.translation.SelectableExporters;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.control.util.PreferenceAccess;
import org.hwyl.sexytopo.demo.TestSurveyCreator;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Base class for all activities that use the action bar.
 */
public abstract class SexyTopoActivity extends AppCompatActivity {


    protected SurveyManager dataManager;

    private static Instrument instrument = Instrument.OTHER;
    private static Communicator comms = NullCommunicator.getInstance();

    protected static boolean hasStarted = false;
    private static boolean debugMode = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SexyTopo.context = this.getApplicationContext();
        dataManager = SurveyManager.getInstance(this.getApplicationContext());

        // if Android restarts the activity after a crash, force it to go through the startup
        // process :/
        if (!hasStarted && !(this instanceof StartUpActivity)) {
            startActivity(StartUpActivity.class);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        SexyTopo.context = this.getApplicationContext();
        setOrientation();

        // this causes the request to happen twice because it is called by StartupActivity, then
        // immediately when the activity is started... something to fix sometime
        requestPermissionsIfRequired();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setOrientation();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.action_bar, menu);

        InputMode inputMode = getInputMode();
        MenuItem inputModeMenuItem = menu.findItem(inputMode.getMenuId());
        inputModeMenuItem.setChecked(true);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // update Instruments menu with any additional options
        // provided by the connected Instrument's Communicator
        MenuItem deviceMenu = menu.findItem(R.id.action_device_menu);
        SubMenu subMenu = deviceMenu.getSubMenu();
        subMenu.clear();
        subMenu.add(Menu.NONE, R.id.action_device_connect, 0, getString(R.string.action_device_connect));
        Map<Integer, String> commands = requestComms().getCustomCommands();
        for (Map.Entry<Integer, String> entry: commands.entrySet()) {
            int id = entry.getKey();
            String name = entry.getValue();
            subMenu.add(Menu.NONE, id, 0, name);
        }

        boolean isDevMenuVisible = PreferenceAccess.getBoolean(
                this,"pref_key_developer_mode", false);
        MenuItem devMenu = menu.findItem(R.id.action_dev_menu);
        devMenu.setVisible(isDevMenuVisible);

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        // this has to be a big hairy if-else chain instead of a switch statement
        // (itemId is no longer a constant in later Android versions)
        if (itemId == R.id.action_save) {
            requestSave();
            return true;
        } else if (itemId == R.id.action_device_connect) {
            startActivity(DeviceActivity.class);
            return true;
        } else if (itemId == R.id.action_table) {
            startActivity(TableActivity.class);
            return true;
        } else if (itemId == R.id.action_plan) {
            startActivity(PlanActivity.class);
            return true;
        } else if (itemId == R.id.action_elevation) {
            startActivity(ExtendedElevationActivity.class);
            return true;
        } else if (itemId == R.id.action_survey) {
            startActivity(StatsActivity.class);
            return true;
        } else if (itemId == R.id.action_trip) {
            startActivity(TripActivity.class);
            return true;
        } else if (itemId == R.id.action_settings) {
            startActivity(SettingsActivity.class);
            return true;
        } else if (itemId == R.id.action_help) {
            startActivity(GuideActivity.class);
            return true;
        } else if (itemId == R.id.action_about) {
            openAboutDialog();
            return true;
        } else if (
                itemId == R.id.action_input_mode_forward ||
                itemId == R.id.action_input_mode_backward ||
                itemId == R.id.action_input_mode_combo ||
                itemId == R.id.action_input_mode_cal_check) {
            setInputModePreference(item);
            return true;
        } else if (itemId == R.id.action_file_new) {
            confirmToProceedIfNotSaved("requestStartNewSurvey");
            return true;
        } else if (itemId == R.id.action_file_open) {
            confirmToProceedIfNotSaved("requestOpenSurvey");
            return true;
        } else if (itemId == R.id.action_file_delete) {
            deleteSurvey();
            return true;
        } else if (itemId == R.id.action_file_save) {
            requestSave();
            return true;
        } else if (itemId == R.id.action_file_save_as) {
            requestSaveAs();
            return true;
        } else if (itemId == R.id.action_file_import) {
            confirmToProceedIfNotSaved("requestImportSurvey");
            return true;
        } else if (itemId == R.id.action_file_export) {
            confirmToProceedIfNotSaved("requestExportSurvey");
            return true;
        } else if (itemId == R.id.action_file_restore_autosave) {
            restoreAutosave();
            return true;
        } else if (itemId == R.id.action_file_exit) {
            confirmToProceedIfNotSaved(R.string.exit_question, "exit");
            return true;
        } else if (itemId == R.id.action_undo_last_leg) {
            undoLastLeg();
            return true;
        } else if (itemId == R.id.action_link_survey) {
            confirmToProceedIfNotSaved("requestLinkExistingSurvey");
            return true;
        } else if (itemId == R.id.action_system_log) {
            startActivity(SystemLogActivity.class);
            return true;
        } else if (itemId == R.id.action_generate_test_survey) {
            generateTestSurvey();
            return true;
        } else if (itemId == R.id.action_debug_mode) {
            toggleDebugMode();
            return true;
        } else if (itemId == R.id.action_kill_connection) {
            killConnection();
            return true;
        } else if (itemId == R.id.action_force_crash) {
            forceCrash();
            return true;
        } else {
            boolean handled = requestComms().handleCustomCommand(itemId);
            if (handled) {
                return true;
            } else {
                return super.onOptionsItemSelected(item);
            }
        }

    }


    private void requestPermissionsIfRequired() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return; // no need to request permissions for earlier Android versions
        }
        String[] desiredPermissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        List<String> notYetGotPermissions = new ArrayList<>(Arrays.asList(desiredPermissions));
        for (String permission : desiredPermissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notYetGotPermissions.add(permission);
            }

        }

        if (!notYetGotPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                notYetGotPermissions.toArray(new String[]{}),
                0);
        }

    }


    protected Instrument getInstrument() {
        return instrument;
    }

    protected void setInstrument(Instrument instrument) {
        SexyTopoActivity.instrument = instrument;
    }


    protected Communicator requestComms() {
        return comms;
    }

    protected void setComms(Communicator communicator) {
        SexyTopoActivity.comms = communicator;
    }


    protected void initialiseData() {
        try {
            if (isThereAnActiveSurvey()) {
                loadActiveSurvey();
            } else {
                createNewActiveSurvey();
            }
        } catch (Exception exception) {
            Log.e(exception);
            helpThereSeemsToBeNoSurveyDoSomething();
        }

    }

    private boolean isThereAnActiveSurvey() {
        return getPreferences().contains(SexyTopo.PREFERENCE_ACTIVE_SURVEY_URI);
    }

    public void loadActiveSurvey() {

        String activeSurveyUriString = PreferenceAccess.getString(
                this, SexyTopo.PREFERENCE_ACTIVE_SURVEY_URI, "Error");
        Log.d("Active survey is <i>" + activeSurveyUriString + "</i>");

        Uri activeSurveyUri = Uri.parse(activeSurveyUriString);
        DocumentFile surveyDirectory = DocumentFile.fromTreeUri(this, activeSurveyUri);

        // Create a temp survey without laoding the data just for
        Survey placeholder = new Survey();
        placeholder.setDirectory(surveyDirectory);

        if (!IoUtils.doesSurveyExist(this, activeSurveyUri)) {
            Log.e("Survey at " + activeSurveyUri + " does not exist");
            helpThereSeemsToBeNoSurveyDoSomething();

        } else if (isAutosaveNewerThanProperSave(this, placeholder)) {
            restoreAutosave(surveyDirectory);
        } else {
            loadSurvey(surveyDirectory);
        }
    }

    private boolean isAutosaveNewerThanProperSave(Context context, Survey survey) {

        DocumentFile dataFile = IoUtils.getDataFile(context, survey);
        DocumentFile autosaveFile = IoUtils.getAutosaveDataFile(context, survey);

        if (!autosaveFile.exists()) {
            return false;
        } else {
            Log.d("Noticed an autosave file");
        }

        if (!dataFile.exists()) {
            Log.d("Data file seems to be missing!?");
            return true;

        } else if (autosaveFile.lastModified() > dataFile.lastModified()) {
            Log.d("Autosave file is newer than data file");
            return true;
        }

        return false;
    }


    private void helpThereSeemsToBeNoSurveyDoSomething() {
        createNewActiveSurvey();
    }


    private void createNewActiveSurvey() {
        Survey survey = new Survey();
        setSurvey(survey);
    }

    private void openAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.about_dialog, null, false);

        String version = getVersionName(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(R.drawable.laser_icon)
                .setTitle(getText(R.string.app_name) + " v" + version)
                .setNeutralButton(R.string.ok, null)
                .setView(messageView);
        builder.create().show();

    }


    @SuppressLint("UnusedDeclaration")
    public void requestExportSurvey() {  // public due to stupid Reflection requirements

        final Survey survey = getSurvey();

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                this);

        builderSingle.setTitle(getString(R.string.select_export_type));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);

        final Map<String, Exporter> nameToExporter = new HashMap<>();

        for (Exporter exporter : SelectableExporters.EXPORTERS) {
            String name = exporter.getExportTypeName(this);
            arrayAdapter.add(name);
            nameToExporter.put(name, exporter);
        }

        builderSingle.setNegativeButton(getString(R.string.cancel),
                (dialog, which) -> dialog.dismiss());

        builderSingle.setAdapter(arrayAdapter,
                (dialog, which) -> {
                    String name = arrayAdapter.getItem(which);
                    Exporter selectedExporter = nameToExporter.get(name);
                    try {
                        assert selectedExporter != null;
                        selectedExporter.export(SexyTopoActivity.this, survey);
                        showSimpleToast(survey.getName() + " " +
                                getString(R.string.export_successful));
                    } catch (Exception exception) {
                        showExceptionAndLog(exception);
                    }
                });
        builderSingle.show();
    }


    public static String getVersionName(android.content.Context context) {
        String version;
        try {
            PackageInfo pInfo =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception exception) {
            version = "Unknown";
        }
        return version;
    }


    public static int getVersionCode(android.content.Context context) {
        int version;
        try {
            PackageInfo pInfo =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionCode;
        } catch (Exception exception) {
            version = -1;
        }
        return version;
    }


    @SuppressLint("UnusedDeclaration")
    public void requestLinkExistingSurvey() {  // public due to stupid Reflection requirements
        selectDocumentTree(SexyTopo.REQUEST_CODE_SELECT_SURVEY_TO_LINK);
    }


    private void linkToStationInSurvey(DocumentFile directory) {
        try {
            Survey surveyToLink = Loader.loadSurvey(this, directory);
            linkToStationInSurvey(surveyToLink);
        } catch (Exception exception) {
            showSimpleToast("Couldn't load selected survey: " + exception);
            Log.e(exception);
        }
    }

    private void linkToStationInSurvey(final Survey surveyToLink) {
        final Station[] stations = surveyToLink.getAllStations().toArray(new Station[]{});

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);

        builderSingle.setTitle(getString(R.string.link_survey_station));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);

        for (Station station : stations) {
            arrayAdapter.add(station.getName());
        }

        builderSingle.setNegativeButton(getString(R.string.cancel),
                (dialog, which) -> dialog.dismiss());

        builderSingle.setAdapter(arrayAdapter,
            (dialog, which) -> {
                String stationName = arrayAdapter.getItem(which);
                try {
                    Station selectedStation = Survey.NULL_STATION;
                    for (Station station : stations) {
                        if (station.getName().equals(stationName)) {
                            selectedStation = station;
                            break;
                        }
                    }

                    Survey current = getSurvey();
                    joinSurveys(current, current.getActiveStation(), surveyToLink, selectedStation);

                    try {
                        Saver.save(SexyTopoActivity.this, surveyToLink);
                    } catch (Exception exception) {
                        showSimpleToast("Couldn't save new connection");
                        showExceptionAndLog(exception);
                    }

                } catch (Exception exception) {
                    showExceptionAndLog(exception);
                }
            });
        builderSingle.show();
    }


    protected void startActivity(Class<? extends SexyTopoActivity> clazz) {
        if (! clazz.isInstance(this)) {
            Intent intent = new Intent(this, clazz);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    private void saveSurveyAs(Uri uri) {
        Survey survey = getSurvey();
        DocumentFile directory = DocumentFile.fromTreeUri(this, uri);

        if (directory == null || !directory.exists() || !directory.canWrite()) {
            Exception exception = new Exception("invalid survey directory");
            showExceptionAndLog("WARNING: could not save", exception);
            return;
        }

        if (IoUtils.isDirectoryEmpty(directory)) {
            survey.setDirectory(directory);
            saveSurvey();

        } else if (IoUtils.isSurveyDirectory(directory)) {
            new AlertDialog.Builder(this)
                    .setTitle("Directory contains survey; overwrite?")
                    .setPositiveButton(R.string.overwrite, (dialogInterface, id) -> {
                        survey.setDirectory(directory);
                        saveSurvey();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

        } else {
            new AlertDialog.Builder(this)
                .setTitle("Directory is not empty; save anyway?")
                .setPositiveButton(R.string.save, (dialogInterface, id) -> {
                    survey.setDirectory(directory);
                    saveSurvey();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }

    }

    private void saveSurvey() {
        new SaveTask().execute(this);
    }

    /**
     * This is used to set whether a survey will be reopened when opening SexyTopo
     */
    private void updateRememberedSurvey() {
        Uri uri = getSurvey().getUri();
        PreferenceAccess.setString(this, SexyTopo.PREFERENCE_ACTIVE_SURVEY_URI, uri.toString());
    }

    protected void redraw() {
        // Redraw the graph if we're in a graph view
        View graph = findViewById(R.id.graphView);
        if (graph != null) {
            graph.invalidate();
        }
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public void requestSave() {
        Survey survey = getSurvey();

        // if we know where this survey lives, save it
        if (survey.hasHome()) {
            saveSurvey();

        // else if survey has not been saved anywhere, ask where to save
        } else {
            requestSaveAs();
        }
    }


    protected void requestSaveAs() {
        selectDocumentTree(SexyTopo.REQUEST_CODE_SAVE_AS_SURVEY);
    }

    protected void selectDocumentFile(int requestCode) {
        
    }

    protected void selectDocumentTree(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri uri = getInitialUri();
            if (uri != null) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
            }
        }

        intent = Intent.createChooser(intent, "hello!");
        startActivityForResult(intent, requestCode);
    }

    protected Uri getInitialUri() {
        Uri uri = null;

        try {
            Survey survey = getSurvey();
            uri = IoUtils.getParentUri(survey);

            if (uri == null) {
                uri = IoUtils.getDefaultSurveyUri(this);
            }

        } catch (Exception exception) {
            showExceptionAndLog(exception);
        }

        return uri;
    }


    public void requestStartNewSurvey() {  // public due to stupid Reflection requirements
        Log.d("Starting new survey");
        startNewSurvey();
    }

    protected void startNewSurvey() {
        Survey survey = new Survey();
        setSurvey(survey);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (resultData == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            Exception exception = new Exception(
                    "Error code " + resultCode + " from request code " + requestCode);
            showExceptionAndLog(exception);
            return;
        }

        // The user has selected this URI, so presumbably they want SexyTopo to do something with it
        // Keep hold of this permission so we can do things like display known surveys
        Uri uri = resultData.getData();
        int takeFlags = resultData.getFlags();
        takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(uri, takeFlags);


        switch(requestCode) {

            case SexyTopo.REQUEST_CODE_SAVE_AS_SURVEY:
                saveSurveyAs(uri);
                break;

            case SexyTopo.REQUEST_CODE_OPEN_SURVEY:
                DocumentFile toOpen = DocumentFile.fromTreeUri(this, uri);
                loadSurvey(toOpen);
                break;

            case SexyTopo.REQUEST_CODE_MOVE_SURVEY:
                // FIXME

            case SexyTopo.REQUEST_CODE_IMPORT_SURVEY:
                DocumentFile importFile = DocumentFile.fromTreeUri(this, uri);
                importSurvey(importFile);
                break;

            case SexyTopo.REQUEST_CODE_SELECT_SURVEY_TO_LINK:
                DocumentFile toLink = DocumentFile.fromTreeUri(this, uri);
                linkToStationInSurvey(toLink);
                break;

        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }


    public void continueSurvey(final Station joinPoint) {

        final Survey currentSurvey = getSurvey();

        if (!currentSurvey.isSaved()) {
            showSimpleToast(R.string.cannot_extend_unsaved_survey);
            return;
        }

        Survey newSurvey = new Survey();
        newSurvey.getOrigin().setName(joinPoint.getName());
        joinSurveys(currentSurvey, joinPoint, newSurvey, newSurvey.getOrigin());
        setSurvey(newSurvey);
    }

    private void joinSurveys(Survey currentSurvey, Station currentJoinPoint,
                             Survey newSurvey, Station newJoinPoint) {
        currentSurvey.connect(currentJoinPoint, newSurvey, newJoinPoint);
        newSurvey.connect(newJoinPoint, currentSurvey, currentJoinPoint);
    }


    public void unlinkSurvey(final Station station) {

        try {
            Survey survey = getSurvey();

            Set<SurveyConnection> linked = survey.getConnectedSurveys().get(station);
            if (linked == null || linked.size() < 1) {
                throw new Exception("Can't find any surveys to unlink");
            } else if (linked.size() == 1) {
                SurveyConnection onlyConnection = linked.iterator().next();
                unlinkSurveyConnection(survey, station,
                        onlyConnection.otherSurvey, onlyConnection.stationInOtherSurvey);
            } else {
                chooseSurveyToUnlink(survey, station);
            }
        } catch (Exception exception) {
            showSimpleToast("Error unlinking survey: " + exception.getMessage());
            showExceptionAndLog(exception);
        }
    }


    private void chooseSurveyToUnlink(final Survey survey, final Station station) {

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                this);

        builderSingle.setTitle(getString(R.string.unlink_survey));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);

        final Set<SurveyConnection> connections = survey.getSurveysConnectedTo(station);
        for (SurveyConnection connection : connections) {
            arrayAdapter.add(connection.otherSurvey.getName());
        }

        builderSingle.setNegativeButton(getString(R.string.cancel),
                (dialog, which) -> dialog.dismiss());

        builderSingle.setAdapter(arrayAdapter,
                (dialog, which) -> {
                    String surveyName = arrayAdapter.getItem(which);
                    try {
                        for (SurveyConnection connection : connections) {
                            if (connection.otherSurvey.getName().equals(surveyName)) {
                                Survey to = connection.otherSurvey;
                                Station stationTo = connection.stationInOtherSurvey;
                                unlinkSurveyConnection(survey, station, to, stationTo);

                                return;
                            }
                        }
                        throw new Exception("Couldn't find linked survey");

                    } catch (Exception exception) {
                        showExceptionAndLog(exception);
                    }
                });
        builderSingle.show();
    }



    private void unlinkSurveyConnection(Survey from, Station stationFrom,
                                        Survey to, Station stationTo) throws Exception {
        from.disconnect(stationFrom, to);
        Saver.save(this, from);
        getSurveyManager().broadcastSurveyUpdated();

        try {
            to.disconnect(stationTo, from);
            Saver.save(this, to);
        } catch (Exception exception) {
            // don't do anything; the other survey may have been modified so not much we can do
        }
    }



    private void deleteSurvey() {
        if (!getSurvey().isSaved()) {
            showSimpleToast(getString(R.string.cannot_delete_unsaved_survey));
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_survey_title))
            .setMessage(getString(R.string.dialog_delete_survey_content))
            .setPositiveButton(R.string.delete,
                (dialog, whichButton) -> {
                    try {
                        String surveyName = getSurvey().getName();
                        // IoUtils.deleteSurvey(SexyTopoActivity.this, surveyName);
                        // FIXME commented out !!!!!!
                        startNewSurvey();
                    } catch (Exception e) {
                        showSimpleToast(R.string.error_deleting_survey);
                        Log.e("Error deleting survey: " + e);
                    }
            }).setNegativeButton(getString(R.string.cancel),
                (dialog, whichButton) -> { /* Do nothing */ })
            .show();
    }


    public void requestOpenSurvey() {  // public due to stupid Reflection requirements
        selectDocumentTree(SexyTopo.REQUEST_CODE_OPEN_SURVEY);
    }


    protected void loadSurvey(DocumentFile surveyDirectory) {
        try {
            Log.d("Loading from <i>" + surveyDirectory.getName() + "</i>...");
            Survey survey = Loader.loadSurvey(SexyTopoActivity.this, surveyDirectory);
            getSurveyManager().setCurrentSurvey(survey);
            updateRememberedSurvey();
            startActivity(PlanActivity.class);
            Log.d("Loaded");
            showSimpleToast(getString(R.string.loaded) + " " + survey.getName());
        } catch (Exception exception) {
            showExceptionAndLog(exception);
        }

    }


    protected void restoreAutosave(DocumentFile surveyDirectory) {
        try {
            Survey survey = Loader.restoreAutosave(SexyTopoActivity.this, surveyDirectory);
            getSurveyManager().setCurrentSurvey(survey);
            showSimpleToast(getString(R.string.restored));
        } catch (Exception exception) {
            showExceptionAndLog(exception);
        }
    }


    private void restoreAutosave() {
        Survey survey = getSurvey();
        DocumentFile surveyDirectory = IoUtils.getSurveyDirectory(this, survey);
        restoreAutosave(surveyDirectory);
    }


    public void exit() {
        finishAffinity();
        System.exit(0);
    }

    @SuppressLint("UnusedDeclaration")
    public void requestImportSurvey() { // public due to stupid Reflection requirements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selectDocumentTree(SexyTopo.REQUEST_CODE_IMPORT_SURVEY);
        }
    }

    protected void importSurvey(DocumentFile file) {
        try {
            Survey survey = ImportManager.toSurvey(this, file);
            survey.checkSurveyIntegrity();
            ImportManager.saveACopyOfSourceInput(this, survey, file);
            getSurveyManager().setCurrentSurvey(survey);
            showSimpleToast("Import complete (not yet saved)");

        } catch (Exception exception) {
            showExceptionAndLog("Failed to import survey", exception);
        }
    }

    protected void confirmToProceed(
            int titleId, int messageId, int confirmId, int cancelId,
            final String methodToCallIfProceeding,
            final Object... args) {

        new AlertDialog.Builder(this)
            .setTitle(getString(titleId))
            .setMessage(getString(messageId))
            .setPositiveButton(confirmId,
                    (dialog, whichButton) -> invokeMethod(methodToCallIfProceeding, args))
            .setNegativeButton(getString(cancelId), null)
            .show();
    }


    protected void confirmToProceedIfNotSaved(final String methodToCallIfProceeding) {
        confirmToProceedIfNotSaved(R.string.continue_question, methodToCallIfProceeding);
    }


    protected void confirmToProceedIfNotSaved(
            int continueMessageId,
            String methodToCallIfProceeding) {



        if (getSurvey().isSaved()) {
            invokeMethod(methodToCallIfProceeding);
            return;
        }

        confirmToProceed(
                continueMessageId,
                R.string.warning_survey_not_saved,
                R.string.carry_on,
                R.string.cancel,
                methodToCallIfProceeding);
    }

    protected void invokeMethod(String name, Object... args) {
        try {
            List<Class<?>> classes = new ArrayList<>();
            for (Object arg : args) {
                classes.add(arg.getClass());
            }
            Method method = getClass().getMethod(name, classes.toArray(new Class[]{}));
            method.invoke(this, args);

        } catch (Exception exception) {
            showExceptionAndLog(exception);
        }
    }

    private void undoLastLeg() {
        getSurvey().undoAddLeg();
        getSurveyManager().broadcastSurveyUpdated();

    }

    private void setInputModePreference(MenuItem item) {
        item.setChecked(!item.isChecked());
        SharedPreferences preferences =
                getSharedPreferences(SexyTopo.GENERAL_PREFS, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        int id = item.getItemId();
        InputMode inputMode = InputMode.byMenuId(id);
        editor.putString(SexyTopo.INPUT_MODE_PREFERENCE, inputMode.name());
        editor.apply();
    }

    private void generateTestSurvey() {
        new AlertDialog.Builder(this)
                .setTitle("Generate Test Data")
                .setMessage("Replace the existing survey with randomly-generated data?")
                .setCancelable(false)
                .setPositiveButton("Replace", (dialog, id) -> {
                    try {
                        Survey currentSurvey = TestSurveyCreator.create(10, 5);
                        setSurvey(currentSurvey);
                    } catch (Exception exception) {
                        showExceptionAndLog(exception);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();

    }

    private void toggleDebugMode() {
        debugMode = !debugMode;
        dataManager.broadcastSurveyUpdated();
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    @SuppressLint("deprecated")
    private void killConnection() {
        try {
            showSimpleToast(R.string.killing_comms_process);
            comms.forceStop();
        } catch (Exception e) {
            Log.e("problem when trying to kill connection: " + e);
        }
    }


    private void forceCrash() {
        throw new RuntimeException("Boom! Forced crash requested(!)");
    }


    protected Survey getSurvey() {
        return getSurveyManager().getCurrentSurvey();
    }


    protected void setSurvey(Survey survey) {
        getSurveyManager().setCurrentSurvey(survey);
    }

    public SurveyManager getSurveyManager() {
        return dataManager;
    }


    protected InputMode getInputMode() {
        return getSurveyManager().getInputMode();
    }


    public void showSimpleToast(String message) {
        if (!isFinishing()) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            Log.d("Shown: " + message);
        }
    }

    public void showSimpleToast(int id) {
        showSimpleToast(getString(id));
    }

    protected void showExceptionAndLog(Exception exception) {
        String prefix = getString(R.string.error_prefix);
        showExceptionAndLog(prefix, exception);
    }

    protected void showExceptionAndLog(String prefix, Exception exception) {
        Log.e(exception);
        showSimpleToast(prefix + " " + exception.getMessage());
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void setOrientation() {
        String orientationPreference = PreferenceAccess.getString(
                this, "pref_orientation", "");

        if (orientationPreference.equals("Force Portrait")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (orientationPreference.equals("Force Landscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }


    public void jumpToStation(Station station, Class<? extends SexyTopoActivity> clazz) {
        Intent intent = new Intent(this, clazz);
        Bundle bundle = new Bundle();
        bundle.putString(SexyTopo.JUMP_TO_STATION, station.getName());
        intent.putExtras(bundle);
        startActivity(intent);
    }


    private class SaveTask extends AsyncTask<android.content.Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... contexts) {
            try {
                android.content.Context context = contexts[0];
                Survey survey = getSurvey();
                Saver.save(context, survey);
                return true;
            } catch (Exception exception) {
                Log.e(exception);
                try {
                    FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
                    crashlytics.recordException(exception);
                    Log.e(exception);
                } catch (Exception inner) {
                    Log.e(inner);
                }
                return false;
            }
        }


        @Override
        protected void onPostExecute(Boolean wasSuccessful) {
            if (wasSuccessful) {
                updateRememberedSurvey();
                SexyTopoActivity.this.redraw();
                showSimpleToast(R.string.survey_saved);
            } else {
                showSimpleToast(getString(R.string.error_saving_survey));
            }
        }
    }

}
