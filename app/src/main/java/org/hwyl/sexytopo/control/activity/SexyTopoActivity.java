package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.documentfile.provider.DocumentFile;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.Instrument;
import org.hwyl.sexytopo.comms.missing.NullCommunicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SexyTopoPermissions;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.StartLocation;
import org.hwyl.sexytopo.control.io.SurveyDirectory;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.control.io.translation.ImportManager;
import org.hwyl.sexytopo.control.io.translation.SelectableExporters;
import org.hwyl.sexytopo.control.components.StationSelectorDialog;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Base class for all activities that use the action bar.
 */
public abstract class SexyTopoActivity extends AppCompatActivity {


    protected SurveyManager dataManager;

    protected static Instrument instrument;

    private static Communicator comms = NullCommunicator.getInstance();

    protected static boolean hasStarted = false;
    private static boolean debugMode = false;

    private String pendingLinkStation = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display - allows content to extend behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Set icon colors for readability based on theme
        boolean isDarkMode = isDarkModeActive();
        View decor = getWindow().getDecorView();

        // Use WindowInsetsController for icon colors (API 30+)
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), decor);
        insetsController.setAppearanceLightStatusBars(!isDarkMode);
        insetsController.setAppearanceLightNavigationBars(!isDarkMode);

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
        requestPermissionsIfRequired();
    }

    /**
     * Helper method to set up MaterialToolbar for menu handling.
     * Call this in onCreate after setContentView if using a MaterialToolbar.
     */
    protected void setupMaterialToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    /**
     * Helper method to apply window inset padding to a view for edge-to-edge layout.
     * Handles both status bar and navigation bar insets.
     * Can be called from onCreate of any activity.
     *
     * @param viewId the ID of the root view to apply insets to
     * @param applyTopInset whether to apply status bar inset to top padding
     * @param applyBottomInset whether to apply navigation bar inset to bottom padding
     */
    protected void applyEdgeToEdgeInsets(int viewId, boolean applyTopInset, boolean applyBottomInset) {
        View view = findViewById(viewId);
        if (view != null) {
            final int left = view.getPaddingLeft();
            final int top = view.getPaddingTop();
            final int right = view.getPaddingRight();
            final int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                int topInset = applyTopInset ? insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()).top : 0;
                int bottomInset = applyBottomInset ? insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()).bottom : 0;
                v.setPadding(left, top + topInset, right, bottom + bottomInset);
                return insets;
            });
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        setTheme();
        setOrientation();

        // Reapply system bar colors after theme changes
        int panelColor = ContextCompat.getColor(this, R.color.panelBackground);
        getWindow().setStatusBarColor(panelColor);
        getWindow().setNavigationBarColor(panelColor);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.action_bar, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);

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
        if (subMenu != null) {
            subMenu.clear();
            subMenu.add(Menu.NONE, R.id.action_device_connect, 0, R.string.action_device_connect);
            Map<Integer, Integer> commands = requestComms().getCustomCommands();
            for (Map.Entry<Integer, Integer> entry: commands.entrySet()) {
                int id = entry.getKey();
                int stringId = entry.getValue();
                String name = getString(stringId);
                subMenu.add(Menu.NONE, id, 0, name);
            }
        }

        boolean isDevMenuVisible = GeneralPreferences.isDevModeOn();
        MenuItem devMenu = menu.findItem(R.id.action_dev_menu);
        devMenu.setVisible(isDevMenuVisible);

        // update Fullscreen menu item checked state
        MenuItem fullscreenItem = menu.findItem(R.id.action_fullscreen);
        if (fullscreenItem != null) {
            fullscreenItem.setChecked(GeneralPreferences.isImmersiveModeOn());
        }

        // update Connection menu item state
        MenuItem connectItem = menu.findItem(R.id.action_connect);
        if (connectItem != null) {
            boolean hasInstrument = !(requestComms() instanceof NullCommunicator);
            connectItem.setEnabled(hasInstrument);
            connectItem.setChecked(hasInstrument && requestComms().isConnected());
        }

        // disable Find Station by default
        MenuItem findStationItem = menu.findItem(R.id.action_find_station);
        if (findStationItem != null) {
            findStationItem.setEnabled(false);
        }

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
        } else if (itemId == R.id.action_stats) {
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
        } else if (itemId == R.id.action_file_save) {
            requestSave();
            return true;
        } else if (itemId == R.id.action_file_save_as) {
            requestSaveAs();
            return true;
        } else if (itemId == R.id.action_file_delete) {
            requestDelete();
            return true;
        } else if (itemId == R.id.action_file_restore_autosave) {
            confirmToProceedIfNotSaved("requestRestoreAutosave");
            return true;
        } else if (itemId == R.id.action_file_import_file) {
            confirmToProceedIfNotSaved("requestImportSurveyFile");
            return true;
        } else if (itemId == R.id.action_file_import_directory) {
            confirmToProceedIfNotSaved("requestImportSurveyDirectory");
            return true;
        } else if (itemId == R.id.action_file_export) {
            confirmToProceedIfNotSaved("requestExportSurvey");
            return true;
        } else if (itemId == R.id.action_file_exit) {
            confirmToProceedIfNotSaved(R.string.exit_question, "requestExit");
            return true;
        } else if (itemId == R.id.action_undo_last_leg) {
            undoLastLeg();
            return true;
        } else if (itemId == R.id.action_find_station) {
            onFindStation();
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
        } else if (itemId == R.id.action_trigger_autosave) {
            getSurvey().setAutosaved(false);
            getSurveyManager().autosave();
            return true;
        }else if (itemId == R.id.action_kill_connection) {
            killConnection();
            return true;
        } else if (itemId == R.id.action_force_crash) {
            forceCrash();
            return true;
        } else if (itemId == R.id.action_connect) {
            toggleConnection();
            return true;
        } else if (itemId == R.id.action_fullscreen) {
            toggleImmersiveMode();
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


    // ***************  Top-level user-requested actions  ***************

    @SuppressLint("UnusedDeclaration") // called through Reflection
    public void requestStartNewSurvey() {
        startNewSurvey();
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
        // Would like to use createDirectory here, but Androids devs are incompetent
        selectDirectory(
            SexyTopoConstants.REQUEST_CODE_SAVE_AS_SURVEY,
            StartLocation.TOP_LEVEL,
            R.string.file_intent_save_as_title);
    }

    @SuppressLint("UnusedDeclaration") // called through Reflection
    public void requestOpenSurvey() {
        selectDirectory(
            SexyTopoConstants.REQUEST_CODE_OPEN_SURVEY,
            StartLocation.SURVEY_PARENT,
            R.string.file_intent_open_title);
    }


    @SuppressLint("UnusedDeclaration") // called through Reflection
    public void requestLinkExistingSurveyToStation(Station station) {
        pendingLinkStation = station.getName();
        selectDirectory(
            SexyTopoConstants.REQUEST_CODE_SELECT_SURVEY_TO_LINK,
            StartLocation.SURVEY_PARENT,
            R.string.file_intent_link_title);
    }

    @SuppressLint("UnusedDeclaration") // called through Reflection
    public void requestRename() {
        /* Currently unused...
        Survey survey = getSurvey();
        DocumentFile directory = survey.getDirectory();
        directory.renameTo("New Name");
        getSurveyManager().broadcastSurveyUpdated();
        */
    }

    @SuppressLint("UnusedDeclaration") // called through Reflection
    public void requestRestoreAutosave() {
        Survey survey = getSurvey();
        DocumentFile directory = SurveyDirectory.TOP.get(survey).getDocumentFile(this);

        try {
            restoreAutosave(directory);
        } catch (Exception exception) {
            showExceptionAndLog(R.string.file_autosave_load_error, exception);
        }
    }


    @SuppressLint("UnusedDeclaration")  // called through Reflection
    public void requestImportSurveyFile() {
        selectFile(
                SexyTopoConstants.REQUEST_CODE_IMPORT_SURVEY_FILE,
                StartLocation.TOP_LEVEL,
                R.string.file_intent_import_select_source);
    }

    @SuppressLint("UnusedDeclaration")  // called through Reflection
    public void requestImportSurveyDirectory() {
        selectDirectory(
                SexyTopoConstants.REQUEST_CODE_IMPORT_SURVEY_DIRECTORY,
                StartLocation.TOP_LEVEL,
                R.string.file_intent_import_select_source);
    }

    public void requestDelete() {
        selectDirectory(
            SexyTopoConstants.REQUEST_CODE_DELETE_SURVEY_DIRECTORY,
            StartLocation.TOP_LEVEL,
            R.string.file_intent_delete_select_target);
    }

    @SuppressLint("UnusedDeclaration")  // called through Reflection
    public void requestExit() {
        finishAffinity();
        System.exit(0);
    }

    private void requestPermissionsIfRequired() {

        List<String> notYetGotPermissions = new ArrayList<>();
        for (String permission : SexyTopoPermissions.getPermissions()) {
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


    protected void startNewSurvey() {
        Survey survey = new Survey();
        setSurvey(survey);
        Log.i(R.string.file_started_new_survey);
    }


    private void openAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.about_dialog, null, false);

        String version = getVersionName(this);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.laser_icon)
                .setTitle(getText(R.string.app_name) + " v" + version)
                .setNeutralButton(R.string.ok, null)
                .setView(messageView);
        builder.create().show();

    }


    @SuppressLint("UnusedDeclaration")
    public void requestExportSurvey() {  // public due to stupid Reflection requirements

        MaterialAlertDialogBuilder builderSingle = new MaterialAlertDialogBuilder(this);

        builderSingle.setTitle(R.string.export_select_type);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);

        arrayAdapter.addAll(SelectableExporters.getExportTypeNames(this));
        builderSingle.setNegativeButton(R.string.cancel,
                (dialog, which) -> dialog.dismiss());

        builderSingle.setAdapter(arrayAdapter,
                (dialog, which) -> {
                    String name = arrayAdapter.getItem(which);
                    Exporter exporter = SelectableExporters.fromName(this, name);
                    exportSurvey(exporter);
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

    private void linkToStationInSurvey(DocumentFile directory) {
        try {
            Survey surveyToLink = Loader.loadSurvey(this, directory);
            linkToStationInSurvey(surveyToLink);
        } catch (Exception exception) {
            showExceptionAndLog(R.string.file_load_survey_error, exception);
        }
    }

    private void linkToStationInSurvey(final Survey surveyToLink) {
        StationSelectorDialog.show(
            this,
            surveyToLink,
            R.string.file_link_survey_station,
            R.string.tool_find_station_dialog_hint,
            android.R.string.ok,
            selectedStation -> {
                try {
                    Survey current = getSurvey();
                    Station stationToLink = pendingLinkStation != null ?
                        current.getStationByName(pendingLinkStation) : null;
                    if (stationToLink != null) {
                        joinSurveys(current, stationToLink, surveyToLink, selectedStation);
                    }
                    pendingLinkStation = null;

                    // Save both surveys so the connection is persisted in both directions
                    try {
                        Saver.save(SexyTopoActivity.this, current);
                        Saver.save(SexyTopoActivity.this, surveyToLink);
                    } catch (Exception exception) {
                        showExceptionAndLog(R.string.file_link_survey_save_error, exception);
                    }

                    getSurveyManager().broadcastSurveyUpdated();

                } catch (Exception exception) {
                    showExceptionAndLog(exception);
                }
            }
        );
    }


    protected void startActivity(Class<? extends SexyTopoActivity> clazz) {
        if (! clazz.isInstance(this)) {
            Intent intent = new Intent(this, clazz);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    private void saveSurveyAs(DocumentFile directory) {
        Survey survey = getSurvey();

        if (directory == null || !directory.exists() || !directory.canWrite()) {
            Exception exception = new Exception(getString(R.string.file_save_error_invalid_folder));
            showExceptionAndLog(R.string.file_save_survey_error, exception);
            return;
        }

        if (IoUtils.isDirectoryEmpty(directory)) {
            survey.setDirectory(directory);
            saveSurvey();

        } else if (IoUtils.isSurveyDirectory(directory)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.file_save_overwrite_survey_title_question)
                    .setPositiveButton(R.string.overwrite, (dialogInterface, id) -> {
                        survey.setDirectory(directory);
                        saveSurvey();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

        } else {
            new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.file_save_to_non_empty_directory_question))
                .setPositiveButton(R.string.save, (dialogInterface, id) -> {
                    survey.setDirectory(directory);
                    saveSurvey();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }

    }

    protected void saveSurvey() {
        new SaveTask().execute(this);
    }

    /**
     * This is used to set whether a survey will be reopened when opening SexyTopo
     */
    private void updateRememberedSurvey() {
        Uri uri = getSurvey().getUri();
        GeneralPreferences.setActiveSurveyUri(uri);
    }

    protected void redraw() {
        // Redraw the graph if we're in a graph view
        View graph = findViewById(R.id.graphView);
        if (graph != null) {
            graph.invalidate();
        }
    }


    @SuppressWarnings({"CommentedOutCode", "UnusedDeclaration"})
    protected void createDirectory(
            int requestCode, StartLocation startLocation, Integer stringId) {
        throw new UnsupportedOperationException(
                "CreateDirectory doesn't work because Android are fucking incompetent");

        // This "works", as in gets the user to create a directory, but results in
        // a URI that cannot be used to get a DocumentFile that is usable as a directory.
        // Fuck you Google for this half-arsed scoped storage shit.
        // Maybe we'll figure out a workaround to use this...
        // Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        // intent.setType(DocumentsContract.Document.MIME_TYPE_DIR);
        // startFileOperation(intent, requestCode, startLocation, stringId);
    }

    protected void selectDirectory(
            int requestCode, StartLocation startLocation, Integer stringId) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startFileOperation(intent, requestCode, startLocation, stringId);
    }

    protected void createFile(int requestCode, StartLocation startLocation, String mimeType, Integer stringId) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType(mimeType);
        startFileOperation(intent, requestCode, startLocation, stringId);
    }

    protected void selectFile(int requestCode, StartLocation startLocation, Integer stringId) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        startFileOperation(intent, requestCode, startLocation, stringId);
    }

    private void startFileOperation(
            Intent intent,
            int requestCode,
            StartLocation startLocation,
            Integer stringId) {

        try {
            if (stringId != null) {
                String message = getString(stringId);

                // This is apparently how you're supposed to it,
                // but it doesn't seem to work...
                // intent.putExtra(Intent.EXTRA_TITLE, title);
                // intent = Intent.createChooser(intent, title);

                // ...so (temp hack) pop up message instead
                // to give the user some idea of what to do
                showSimpleToast(message);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setInitialUri(startLocation, intent);
            }

            startActivityForResult(intent, requestCode);

        } catch (Exception exception) {
            showExceptionAndLog(R.string.file_operation_initialisation_error, exception);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void setInitialUri(StartLocation startLocation, Intent intent) {

        switch (startLocation) {

            case TOP_LEVEL:
                Uri uri = IoUtils.getDefaultSurveyUri(this);
                if (uri == null) {
                    intent.putExtra(
                            DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);
                } else {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                }
                break;

            case SURVEY_PARENT:
                Survey survey = getSurvey();
                Uri parentUri = IoUtils.getParentUri(survey);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, parentUri);
                break;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (resultData == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            Exception exception = new Exception(
                    getString(R.string.request_code_error, resultCode, requestCode));
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

            case SexyTopoConstants.REQUEST_CODE_SAVE_AS_SURVEY:
                DocumentFile toSaveAs = DocumentFile.fromTreeUri(this, uri);
                saveSurveyAs(toSaveAs);
                break;

            case SexyTopoConstants.REQUEST_CODE_OPEN_SURVEY:
                DocumentFile toOpen = DocumentFile.fromTreeUri(this, uri);
                loadSurvey(toOpen);
                break;

            case SexyTopoConstants.REQUEST_CODE_DELETE_SURVEY_DIRECTORY:
                DocumentFile toDelete = DocumentFile.fromTreeUri(this, uri);
                deleteSurvey(toDelete);
                break;

            case SexyTopoConstants.REQUEST_CODE_IMPORT_SURVEY_FILE:
                DocumentFile importFile = DocumentFile.fromSingleUri(this, uri);
                importSurvey(importFile);
                break;

            case SexyTopoConstants.REQUEST_CODE_IMPORT_SURVEY_DIRECTORY:
                DocumentFile importDir = DocumentFile.fromTreeUri(this, uri);
                importSurvey(importDir);
                break;

            case SexyTopoConstants.REQUEST_CODE_SELECT_SURVEY_TO_LINK:
                DocumentFile toLink = DocumentFile.fromTreeUri(this, uri);
                linkToStationInSurvey(toLink);
                break;

        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }


    public void continueSurvey(final Station joinPoint) {
        continueSurvey(joinPoint, joinPoint.getName());
    }

    public void continueSurvey(final Station joinPoint, final String startingStationName) {

        final Survey currentSurvey = getSurvey();

        if (!currentSurvey.isSaved()) {
            showSimpleToast(R.string.file_cannot_extend_unsaved_survey);
            return;
        }

        Survey newSurvey = new Survey();
        newSurvey.getOrigin().setName(startingStationName);
        joinSurveys(currentSurvey, joinPoint, newSurvey, newSurvey.getOrigin());

        // Save the original survey so the connection is persisted in both directions
        try {
            Saver.save(this, currentSurvey);
        } catch (Exception e) {
            Log.e("Error saving original survey after linking: " + e.getMessage());
        }

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
            if (linked == null || linked.isEmpty()) {
                throw new Exception(getString(R.string.file_unlink_survey_no_surveys));
            } else if (linked.size() == 1) {
                SurveyConnection onlyConnection = linked.iterator().next();
                unlinkSurveyConnection(survey, station,
                        onlyConnection.otherSurvey, onlyConnection.stationInOtherSurvey);
            } else {
                chooseSurveyToUnlink(survey, station);
            }

        } catch (Exception exception) {
            showExceptionAndLog(R.string.file_unlink_survey_error, exception);
        }
    }


    private void chooseSurveyToUnlink(final Survey survey, final Station station) {

        MaterialAlertDialogBuilder builderSingle = new MaterialAlertDialogBuilder(
                this);

        builderSingle.setTitle(R.string.file_unlink_survey_dialog_title);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);

        final Set<SurveyConnection> connections = survey.getSurveysConnectedTo(station);
        for (SurveyConnection connection : connections) {
            arrayAdapter.add(connection.otherSurvey.getName());
        }

        builderSingle.setNegativeButton(R.string.cancel,
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
                        throw new Exception(getString(R.string.file_unlink_survey_not_found));

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


    protected void loadSurvey(DocumentFile surveyDirectory) {
        try {
            Log.i(R.string.file_loading_survey, surveyDirectory.getName());
            Survey survey = Loader.loadSurvey(SexyTopoActivity.this, surveyDirectory);
            getSurveyManager().setCurrentSurvey(survey);
            updateRememberedSurvey();
            startActivity(PlanActivity.class);
            showSimpleToast(R.string.file_loaded_survey, survey.getName());

        } catch (Exception exception) {
            showExceptionAndLog(R.string.file_load_survey_error, exception);
        }

    }

    public void deleteSurvey(DocumentFile directory) {

        boolean doesExist = directory != null &&
                IoUtils.doesDirectoryExist(this, directory.getUri());
        boolean isSurvey = doesExist && !IoUtils.isSurveyDirectory(directory);

        if (!isSurvey) {
            showSimpleToast(R.string.file_delete_error_not_survey);
            return;
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.file_dialog_delete_survey_title)
            .setMessage(getString(R.string.file_dialog_delete_survey_content, directory.getName()))
            .setPositiveButton(R.string.delete,
                (dialog, whichButton) -> {
                    try {
                        String name = directory.getName();
                        directory.delete();
                        showSimpleToast(R.string.file_delete_successful, name);
                    } catch (Exception e) {
                        showExceptionAndLog(R.string.file_error_deleting_survey, e);
                    }
                })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }


    protected void restoreAutosave(DocumentFile directory) throws Exception {
        Log.i(R.string.file_restoring_autosave);
        Survey survey = Loader.loadAutosave(this, directory);
        getSurveyManager().setCurrentSurvey(survey);
        showSimpleToast(R.string.file_autosave_restored);

    }


    protected void importSurvey(DocumentFile file) {
        try {
            Survey survey = ImportManager.toSurvey(this, file);
            survey.checkSurveyIntegrity();
            getSurveyManager().setCurrentSurvey(survey);
            showSimpleToast(R.string.import_successful);

        } catch (Exception exception) {
            showExceptionAndLog(R.string.import_failed, exception);
        }
    }

    protected void exportSurvey(Exporter exporter) {
        try {
            Survey survey = getSurvey();
            exporter.export(this, survey);
            showSimpleToast(R.string.export_successful);

        } catch (Exception exception) {
            showExceptionAndLog(R.string.export_failed, exception);
        }
    }

    protected void confirmToProceed(
            int titleId, int messageId, int confirmId, int cancelId,
            final String methodToCallIfProceeding,
            final Object... args) {

        new MaterialAlertDialogBuilder(this)
            .setTitle(titleId)
            .setMessage(messageId)
            .setPositiveButton(confirmId,
                    (dialog, whichButton) -> invokeMethod(methodToCallIfProceeding, args))
            .setNegativeButton(cancelId, null)
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
                R.string.file_warning_survey_not_saved,
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

    protected void onFindStation() {
        // Override in SurveyEditorActivity
    }

    private void setInputModePreference(MenuItem item) {
        item.setChecked(!item.isChecked());
        SharedPreferences preferences =
                getSharedPreferences(SexyTopoConstants.GENERAL_PREFS, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        int id = item.getItemId();
        InputMode inputMode = InputMode.byMenuId(id);
        editor.putString(SexyTopoConstants.INPUT_MODE_PREFERENCE, inputMode.name());
        editor.apply();
    }

    private void generateTestSurvey() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tool_generate_test_title)
                .setMessage(R.string.tool_generate_test_question)
                .setCancelable(false)
                .setPositiveButton(R.string.replace, (dialog, id) -> {
                    try {
                        Survey currentSurvey = ExampleSurveyCreator.create();
                        setSurvey(currentSurvey);
                    } catch (Exception exception) {
                        showExceptionAndLog(R.string.tool_generate_test_error, exception);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
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
        } catch (Exception exception) {
            showExceptionAndLog(R.string.tool_force_kill_error, exception);
        }
    }


    private void forceCrash() {
        throw new RuntimeException(getString(R.string.tool_force_crash_message));
    }

    private void toggleConnection() {
        Communicator communicator = requestComms();

        if (communicator instanceof NullCommunicator) {
            showSimpleToast("No instrument connected");
            return;
        }

        if (communicator.isConnected()) {
            communicator.requestDisconnect();
            showSimpleToast(getString(R.string.device_connection_stop_requested));
        } else {
            communicator.requestConnect();
            showSimpleToast(getString(R.string.device_connection_requested));
        }
    }

    private void toggleImmersiveMode() {
        boolean isCurrentlyImmersive = GeneralPreferences.isImmersiveModeOn();
        boolean newImmersiveState = !isCurrentlyImmersive;

        GeneralPreferences.setImmersiveMode(newImmersiveState);
        applyImmersiveMode(newImmersiveState);

        String message = newImmersiveState ?
            "Fullscreen mode enabled" : "Fullscreen mode disabled";
        showSimpleToast(message);
    }

    /**
     * Apply immersive mode (hides system UI bars) or restore normal mode.
     * When enabled, swipe from edges to temporarily reveal system bars.
     */
    private void applyImmersiveMode(boolean enabled) {
        View decorView = getWindow().getDecorView();

        if (enabled) {
            // Hide status bar, navigation bar, and action bar
            // Use IMMERSIVE_STICKY so system bars reappear on gesture
            int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(flags);
        } else {
            // Restore normal visibility
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Reapply immersive mode when window regains focus
        // (e.g., after home button press or notification)
        if (hasFocus) {
            applyImmersiveMode(GeneralPreferences.isImmersiveModeOn());
        }
    }


    @SuppressWarnings("AccessStaticViaInstance")
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
            Log.i(R.string.log_shown, message);
        }
    }

    public void showSimpleToast(int id, String ... args) {
        showSimpleToast(getString(id, Arrays.asList(args)));
    }

    public void showExceptionAndLog(Exception exception) {
        String prefix = getString(R.string.error_prefix);
        showExceptionAndLog(prefix, exception);
    }

    public void showExceptionAndLog(int id, Exception exception, String ... args) {
        String prefix = getString(id, Arrays.asList(args));
        showExceptionAndLog(prefix, exception);
    }

    public void showExceptionAndLog(String prefix, Exception exception) {
        Log.e(exception);
        showSimpleToast(prefix + ": " + exception.getMessage());
    }

    @SuppressLint("SourceLockedOrientationActivity")
    protected void setOrientation() {
        String orientationPreference = GeneralPreferences.getOrientationMode();

        if (orientationPreference.equals("portrait")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (orientationPreference.equals("landscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    protected void setTheme() {
        String themeMode = GeneralPreferences.getTheme();
        if ("light".equals(themeMode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if ("dark".equals(themeMode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }


    public void jumpToStation(Station station, Class<? extends SexyTopoActivity> clazz) {
        Intent intent = new Intent(this, clazz);
        Bundle bundle = new Bundle();
        bundle.putString(SexyTopoConstants.JUMP_TO_STATION, station.getName());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    protected boolean isDarkModeActive() {
        int nightModeFlags =
            getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    protected boolean isInPortraitMode() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
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
                showSimpleToast(R.string.file_survey_saved);
            } else {
                showSimpleToast(R.string.file_save_survey_error);
            }
        }
    }

}
