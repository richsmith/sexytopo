package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.control.io.translation.ImportManager;
import org.hwyl.sexytopo.control.io.translation.SelectableExporters;
import org.hwyl.sexytopo.demo.TestSurveyCreator;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Base class for all activities that use the action bar.
 */
public abstract class SexyTopoActivity extends AppCompatActivity {

    protected SurveyManager dataManager;

    public SexyTopoActivity() {
        super();
        dataManager = SurveyManager.getInstance(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        SexyTopo.context = this;
        setOrientation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SexyTopo.context = this;
        setOrientation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.action_bar, menu);

        MenuItem menuItem = menu.findItem(R.id.action_back_measurements);
        SharedPreferences preferences =
                getSharedPreferences(SexyTopo.GENERAL_PREFS, Context.MODE_PRIVATE);
        boolean isSelected =
                preferences.getBoolean(SexyTopo.REVERSE_MEASUREMENTS_PREFERENCE, false);
        menuItem.setChecked(isSelected);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_save:
                saveSurvey();
                return true;
            case R.id.action_device:
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
                startActivity(SurveyActivity.class);
                return true;
            case R.id.action_calibration:
                startActivity(CalibrationActivity.class);
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


            case R.id.action_undo_last_leg:
                undoLastLeg();
                return true;
            case R.id.action_back_measurements:
                setReverseMeasurementsPreference(item);
                return true;
            case R.id.action_link_survey:
                confirmToProceedIfNotSaved("linkExistingSurvey");
                return true;
            case R.id.action_generate_test_survey:
                generateTestSurvey();
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private void openAboutDialog() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about_dialog, null, false);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        //TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
        //int defaultColor = textView.getTextColors().getDefaultColor();
        //textView.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(R.drawable.laser_icon_small)
                .setTitle(getText(R.string.app_name) + " v" + SexyTopo.VERSION)
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


    private void startActivity(Class clazz) {
        if (! clazz.isInstance(this)) {
            Intent intent = new Intent(this, clazz);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    private void saveSurvey() {
        try {
            Saver.save(this, getSurvey());
            updateRememberedSurvey();
            showSimpleToast(R.string.survey_saved);
        } catch (Exception exception) {
            showSimpleToast(getString(R.string.error_saving_survey));
            showException(exception);
        }
    }

    /**
     * This is used to set whether a survey will be reopened when opening SexyTopo
     */
    private void updateRememberedSurvey() {
        SharedPreferences preferences = getPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SexyTopo.ACTIVE_SURVEY_NAME, getSurvey().getName());
        editor.commit();
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
                        try {
                            survey.setName(newName);
                            setSurvey(new Survey(Util.getNextDefaultSurveyName(SexyTopoActivity.this)));
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
                        if (Util.isSurveyNameUnique(SexyTopoActivity.this, name)) {
                            Survey survey = new Survey(name);
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
                            joinSurveys(currentSurvey, joinPoint, newSurvey, newSurvey.getOrigin());
                            setSurvey(newSurvey);
                            /*
                            currentSurvey.connect(joinPoint, newSurvey, newSurvey.getOrigin());
                            newSurvey.connect(newSurvey.getOrigin(), currentSurvey, joinPoint);
                            setSurvey(newSurvey);
                            try {
                                Saver.save(SexyTopoActivity.this, currentSurvey);
                                Saver.save(SexyTopoActivity.this, newSurvey);
                            } catch (Exception exception) {
                                showSimpleToast("Couldn't save new connection");
                                showException(exception);
                            }*/
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
            if (linked.size() < 1) {
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
                            throw new Exception("couldn't find linked survey");

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
        SurveyManager.getInstance(this).broadcastSurveyUpdated();

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
                                    Log.d(SexyTopo.TAG, "Error deleting survey: " + e);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }


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
                        try {
                            Survey survey = Loader.loadSurvey(SexyTopoActivity.this, surveyName);
                            SurveyManager.getInstance(SexyTopoActivity.this).setCurrentSurvey(survey);
                            updateRememberedSurvey();
                            showSimpleToast(getString(R.string.loaded) + " " + surveyName);
                        } catch (Exception exception) {
                            showException(exception);
                        }
                    }
                });
        builderSingle.show();
    }


    private void restoreAutosave() {
        try {
            Survey survey = Loader.loadSurvey(SexyTopoActivity.this, getSurvey().getName());
            SurveyManager.getInstance(SexyTopoActivity.this).setCurrentSurvey(survey);
            showSimpleToast(getString(R.string.restored));
        } catch (Exception exception) {
            showException(exception);
        }
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
        survey.checkActiveStation();
        Saver.save(this, survey);
        ImportManager.saveACopyOfSourceInput(this, survey, file);
        SurveyManager.getInstance(SexyTopoActivity.this).setCurrentSurvey(survey);
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
        if (getSurvey().isSaved()) {
            invokeMethod(methodToCallIfProceeding);
            return;
        }

        confirmToProceed(
                R.string.continue_question,
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
        getSurvey().undoLeg();
        SurveyManager.getInstance(this).broadcastSurveyUpdated();

    }

    private void setReverseMeasurementsPreference(MenuItem item) {
        item.setChecked(!item.isChecked());
        SharedPreferences preferences = getSharedPreferences(SexyTopo.GENERAL_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SexyTopo.REVERSE_MEASUREMENTS_PREFERENCE, item.isChecked());
        editor.commit();
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




    protected Survey getSurvey() {
        return SurveyManager.getInstance(this).getCurrentSurvey();
    }


    protected void setSurvey(Survey survey) {
        SurveyManager.getInstance(this).setCurrentSurvey(survey);

    }


    public void showSimpleToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void showSimpleToast(int id) {
        showSimpleToast(getString(id));
    }


    private void showException(Exception exception) {
        Log.d(SexyTopo.TAG, "Error: " + exception);
        showSimpleToast(getString(R.string.error_prefix) + " " + exception.getMessage());
    }

    protected boolean getBooleanPreference(String name) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(name, false);
    }

    protected String getStringPreference(String name) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(name, "");
    }

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

            if (Util.doesSurveyExist(survey.getName())) {
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

}
