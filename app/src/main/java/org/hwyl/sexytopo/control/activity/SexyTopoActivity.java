package org.hwyl.sexytopo.control.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.Instrument;
import org.hwyl.sexytopo.comms.missing.NullCommunicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.control.io.translation.ImportManager;
import org.hwyl.sexytopo.control.io.translation.SelectableExporters;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.demo.TestSurveyCreator;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


/**
 * Base class for all activities that use the action bar.
 */
public abstract class SexyTopoActivity extends AppCompatActivity {


    @SuppressLint("StaticFieldLeak")
    protected static SurveyManager dataManager;

    private static Instrument instrument = Instrument.OTHER;
    private static Communicator comms = NullCommunicator.getInstance();

    protected static boolean hasStarted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SexyTopo.context = this.getApplicationContext();
        dataManager = SurveyManager.getInstance(this.getApplicationContext());

        // if Android restarts the activity after a crash, force it to go through the startup
        // process!
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
        // //provided by the connected Instrument's Communicator
        MenuItem item = menu.findItem(R.id.action_device_menu);
        SubMenu subMenu = item.getSubMenu();
        MenuItem connections = menu.findItem(R.id.action_device_connect);
        subMenu.clear();
        subMenu.add(Menu.NONE, R.id.action_device_connect,
                0, getString(R.string.action_device_connect));
        Map<Integer, String> commands = requestComms().getCustomCommands();
        for (Map.Entry<Integer, String> entry: commands.entrySet()) {
            int id = entry.getKey();
            String name = entry.getValue();
            subMenu.add(Menu.NONE, id, 0, name);
        }

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        switch (itemId) {

            case R.id.action_save:
                saveSurvey();
                return true;
            case R.id.action_device_connect:
                startActivity(DeviceActivity.class);
                return true;
            case R.id.action_table:
                startActivity(TableActivity.class);
                return true;
            case R.id.action_plan:
                startActivity(PlanActivity.class);
                return true;
            case R.id.action_elevation:
                startActivity(ExtendedElevationActivity.class);
                return true;
            case R.id.action_survey:
                startActivity(StatsActivity.class);
                return true;
            case R.id.action_trip:
                startActivity(TripActivity.class);
                return true;
            case R.id.action_settings:
                startActivity(SettingsActivity.class);
                return true;
            case R.id.action_help:
                startActivity(GuideActivity.class);
                return true;
            case R.id.action_about:
                openAboutDialog();
                return true;

            case R.id.action_input_mode_forward:
            case R.id.action_input_mode_backward:
            case R.id.action_input_mode_combo:
            case R.id.action_input_mode_cal_check:
                setInputModePreference(item);
                return true;

            case R.id.action_file_new:
                confirmToProceedIfNotSaved("startNewSurvey");
                return true;
            case R.id.action_file_open:
                confirmToProceedIfNotSaved("openSurvey");
                return true;
            case R.id.action_file_delete:
                deleteSurvey();
                return true;
            case R.id.action_file_save:
                saveSurvey();
                return true;
            case R.id.action_file_save_as:
                saveSurveyAsName();
                return true;
            case R.id.action_file_import:
                confirmToProceedIfNotSaved("importSurvey");
                return true;
            case R.id.action_file_export:
                confirmToProceedIfNotSaved("exportSurvey");
                return true;
            case R.id.action_file_restore_autosave:
                restoreAutosave();
                return true;
            case R.id.action_file_exit:
                confirmToProceedIfNotSaved(R.string.exit_question, "exit");
                return true;


            case R.id.action_undo_last_leg:
                undoLastLeg();
                return true;
            case R.id.action_link_survey:
                confirmToProceedIfNotSaved("linkExistingSurvey");
                return true;


            case R.id.action_system_log:
                startActivity(SystemLogActivity.class);
                return true;
            case R.id.action_generate_test_survey:
                generateTestSurvey();
                return true;
            case R.id.action_force_crash:
                forceCrash();
                return true;

            default:
                boolean handled = requestComms().handleCustomCommand(itemId);
                if (handled) {
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (! Util.isExternalStorageWriteable(this)) {
            showSimpleToast(R.string.external_storage_unwriteable);
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
        //return requestComms(DistoXCommunicator.Protocol.NULL);
        return comms;
    }

    protected void setComms(Communicator communicator) {
        SexyTopoActivity.comms = communicator;
    }


    /*
    protected Communicator requestComms(DistoXCommunicator.Protocol protocol) {

        if (comms == null || comms.getState() == Thread.State.TERMINATED) {
            comms = new DistoXCommunicator(this, dataManager);
        }

        Thread.State commsState = comms.getState();
        if (commsState == Thread.State.NEW) {
           comms.requestStart(protocol);
       } else {
           comms.setProtocol(protocol);
       }

       return comms;
    }*/

    private void openAboutDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.about_dialog, null, false);

        String version = getVersionName(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(R.drawable.laser_icon)
                .setTitle(getText(R.string.app_name) + " v" + version)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setView(messageView);
        builder.create().show();

    }

    public void exportSurvey() {  // public due to stupid Reflection requirements

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
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = arrayAdapter.getItem(which);
                    Exporter selectedExporter = nameToExporter.get(name);
                    try {
                        selectedExporter.export(SexyTopoActivity.this, survey);
                        showSimpleToast(survey.getName() + " " +
                                getString(R.string.export_successful));
                    } catch (Exception exception) {
                        showException(exception);
                    }
                }
            });
        builderSingle.show();
    }


    public static String getVersionName(Context context) {
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


    public static int getVersionCode(Context context) {
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

    public void linkExistingSurvey() {  // public due to stupid Reflection requirements

            File[] surveyDirectories = Util.getSurveyDirectories(this);

            if (surveyDirectories.length == 0) {
                showSimpleToast(getString(R.string.no_surveys));
                return;
            }

            AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);

            builderSingle.setTitle(getString(R.string.link_survey));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.select_dialog_item);

            for (File file : surveyDirectories) {
                arrayAdapter.add(file.getName());
            }

            builderSingle.setNegativeButton(getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            builderSingle.setAdapter(arrayAdapter,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String surveyName = arrayAdapter.getItem(which);
                            try {
                                Survey surveyToLink =
                                        Loader.loadSurvey(SexyTopoActivity.this, surveyName);
                                linkToStationInSurvey(surveyToLink);
                            } catch (Exception exception) {
                                showException(exception);
                            }
                        }
                    });
            builderSingle.show();
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
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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

                        } catch (Exception exception) {
                            showException(exception);
                        }
                    }
                });
        builderSingle.show();
    }


    protected void startActivity(Class clazz) {
        if (! clazz.isInstance(this)) {
            Intent intent = new Intent(this, clazz);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    private void saveSurvey() {
        new SaveTask().execute(this);
    }

    /**
     * This is used to set whether a survey will be reopened when opening SexyTopo
     */
    private void updateRememberedSurvey() {
        SharedPreferences preferences = getPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SexyTopo.ACTIVE_SURVEY_NAME, getSurvey().getName());
        editor.apply();
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }


    private void saveSurveyAsName() {

        final EditText input = new EditText(this);
        input.setText(getSurvey().getName());
        input.setContentDescription("Enter new name");

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_save_as_title))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        String newName = value.toString();
                        Survey survey = getSurvey();
                        String oldName = survey.getName();
                        if (oldName.equals(newName)) {
                            return;
                        }
                        try {
                            survey.setName(newName);
                            if (!Util.isSurveyNameUnique(
                                    SexyTopoActivity.this, survey.getName()))  {
                                throw new Exception("Survey already exists");
                            }
                            Saver.save(SexyTopoActivity.this, survey);
                            updateRememberedSurvey();
                        } catch (Exception exception) {
                            survey.setName(oldName);
                            showSimpleToast(R.string.error_saving_survey);
                            showException(exception);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }


    public void startNewSurvey() {  // public due to stupid Reflection requirements

        Log.d("Starting new survey");

        final EditText input = new EditText(this);
        String defaultName = Util.getNextDefaultSurveyName(this);
        input.setText(defaultName);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_new_survey_title))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        String name = value.toString();
                        Survey survey = new Survey(name);
                        if (Util.isSurveyNameUnique(SexyTopoActivity.this, name)) {
                            setSurvey(survey);
                        } else {
                            showSimpleToast(R.string.dialog_new_survey_name_must_be_unique);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }


    public void continueSurvey(final Station joinPoint) {

        final Survey currentSurvey = getSurvey();

        if (!currentSurvey.isSaved()) {
            showSimpleToast(R.string.cannot_extend_unsaved_survey);
            return;
        }

        final EditText input = new EditText(this);

        String defaultName = Util.getNextAvailableName(this, currentSurvey.getName());
        input.setText(defaultName);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_new_survey_title))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        String name = value.toString();
                        if (Util.isSurveyNameUnique(SexyTopoActivity.this, name)) {
                            Survey newSurvey = new Survey(name);
                            newSurvey.getOrigin().setName(joinPoint.getName());
                            joinSurveys(currentSurvey, joinPoint, newSurvey, newSurvey.getOrigin());
                            setSurvey(newSurvey);
                        } else {
                            showSimpleToast(R.string.dialog_new_survey_name_must_be_unique);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    private void joinSurveys(Survey currentSurvey, Station currentJoinPoint,
                             Survey newSurvey, Station newJoinPoint) {
        currentSurvey.connect(currentJoinPoint, newSurvey, newJoinPoint);
        newSurvey.connect(newJoinPoint, currentSurvey, currentJoinPoint);

        try {
            Saver.save(SexyTopoActivity.this, currentSurvey);
            Saver.save(SexyTopoActivity.this, newSurvey);
        } catch (Exception exception) {
            showSimpleToast("Couldn't save new connection");
            showException(exception);
        }
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
            showException(exception);
        }
    }


    private void chooseSurveyToUnlink(final Survey survey, final Station station) {

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                this);

        builderSingle.setTitle(getString(R.string.unlink_survey));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);

        final Set<SurveyConnection> connections = survey.getConnectedSurveys().get(station);

        for (SurveyConnection connection : connections) {
            arrayAdapter.add(connection.otherSurvey.getName());
        }

        builderSingle.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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
                            showException(exception);
                        }
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
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            String surveyName = getSurvey().getName();
                            Util.deleteSurvey(SexyTopoActivity.this, surveyName);
                            startNewSurvey();
                        } catch (Exception e) {
                            showSimpleToast(R.string.error_deleting_survey);
                            Log.e("Error deleting survey: " + e);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }


    @SuppressLint("UnusedDeclaration")
    public void openSurvey() {  // public due to stupid Reflection requirements

        File[] surveyDirectories = Util.getSurveyDirectories(this);

        if (surveyDirectories.length == 0) {
            showSimpleToast(getString(R.string.no_surveys));
            return;
        }

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                 this);

        builderSingle.setTitle(getString(R.string.open_survey));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);

        for (File file : surveyDirectories) {
            arrayAdapter.add(file.getName());
        }

        builderSingle.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String surveyName = arrayAdapter.getItem(which);
                        loadSurvey(surveyName);
                    }
                });
        builderSingle.show();
    }


    protected void loadSurvey(String surveyName) {
        try {
            Log.d("Loading <i>" + surveyName + "</i>...");
            Survey survey = Loader.loadSurvey(SexyTopoActivity.this, surveyName);
            getSurveyManager().setCurrentSurvey(survey);
            updateRememberedSurvey();
            startActivity(PlanActivity.class);
            Log.d("Loaded");
            showSimpleToast(getString(R.string.loaded) + " " + surveyName);
        } catch (Exception exception) {
            showException(exception);
        }

    }


    protected void restoreAutosave(String name) {
        try {
            Survey survey = Loader.restoreAutosave(SexyTopoActivity.this, name);
            getSurveyManager().setCurrentSurvey(survey);
            showSimpleToast(getString(R.string.restored));
        } catch (Exception exception) {
            showException(exception);
        }
    }


    private void restoreAutosave() {
        String name = getSurvey().getName();
        restoreAutosave(name);
    }


    public void exit() {
        finishAffinity();
        System.exit(0);
    }


    public void importSurvey() { // public due to stupid Reflection requirements

        File[] importFiles = Util.getImportFiles(this);
        if (importFiles.length == 0) {
            showSimpleToast(getString(R.string.no_imports));
            return;
        }

        final Map<String, File> nameToFiles = new HashMap<>();
        for (File file : importFiles) {
            nameToFiles.put(file.getName(), file);
        }

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle(R.string.import_survey);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item);
        arrayAdapter.addAll(nameToFiles.keySet());

        builderSingle.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = nameToFiles.get(arrayAdapter.getItem(which));
                        try {
                            Survey survey = ImportManager.toSurvey(file);

                            if (Util.doesSurveyExist(SexyTopoActivity.this, survey.getName())) {
                                confirmToProceed(
                                        R.string.continue_question,
                                        R.string.survey_already_exists,
                                        R.string.replace,
                                        R.string.cancel,
                                        "saveImportedSurvey", survey, file);
                            } else {
                                saveImportedSurvey(survey, file);
                            }

                        } catch (Exception exception) {
                            showException(exception);
                        }
                    }
                });
        builderSingle.show();
    }

    public void saveImportedSurvey(Survey survey, File file) throws Exception {
        survey.checkSurveyIntegrity();
        Saver.save(this, survey);
        ImportManager.saveACopyOfSourceInput(this, survey, file);
        getSurveyManager().setCurrentSurvey(survey);
        showSimpleToast("Imported " + survey.getName());
    }


    protected void confirmToProceed(
            int titleId, int messageId, int confirmId, int cancelId,
            final String methodToCallIfProceeding,
            final Object... args) {

        new AlertDialog.Builder(this)
                .setTitle(getString(titleId))
                .setMessage(getString(messageId))
                .setPositiveButton(confirmId, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        invokeMethod(methodToCallIfProceeding, args);
                    }
                }).setNegativeButton(getString(cancelId), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
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
            List<Class> classes = new LinkedList<>();
            for (Object arg : args) {
                classes.add(arg.getClass());
            }
            Method method = getClass().getMethod(name, classes.toArray(new Class[]{}));
            method.invoke(this, args);

        } catch (Exception exception) {
            showException(exception);
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
                .setPositiveButton("Replace", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            Survey currentSurvey =
                                    TestSurveyCreator.create(getSurvey().getName(), 10, 5);
                            setSurvey(currentSurvey);
                        } catch (Exception exception) {
                            showException(exception);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();

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
        return SurveyManager.getInstance(this.getApplicationContext());
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


    protected void showException(Exception exception) {
        Log.e(exception);
        showSimpleToast(getString(R.string.error_prefix) + " " + exception.getMessage());
    }

    public boolean getBooleanPreference(String name) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(name, false);
    }

    protected String getStringPreference(String name) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(name, "");
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void setOrientation() {
        String orientationPreference = getStringPreference("pref_orientation");

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

    /*
    private class ImportTask extends AsyncTask<File, Void, Survey> {

        private File file;

        protected Survey doInBackground(File... files) {
            this.file = files[0];

            try {
                return ImportManager.toSurvey(file);
            } catch (Exception exception) {

            }
        }

        protected void onPostExecute(Survey survey) {

            if (SurvexTherionUtil.doesSurveyExist(survey.getName())) {
                confirmToProceed(
                        R.string.continue_question,
                        R.string.survey_already_exists,
                        R.string.replace,
                        R.string.cancel,
                        "saveImportedSurvey", survey, file);
            } else {
                try {
                saveImportedSurvey(survey, file);
                } catch (Exception exception) {

                }
            }

        }
    }*/

    private class SaveTask extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... contexts) {
            try {
                Context context = contexts[0];
                Saver.save(context, getSurvey());
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
                showSimpleToast(R.string.survey_saved);
            } else {
                showSimpleToast(getString(R.string.error_saving_survey));
            }
        }
    }

}
