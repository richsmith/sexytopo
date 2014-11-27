package org.hwyl.sexytopo.control.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.io.Saver;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.Survey;
import org.hwyl.sexytopo.control.SurveyManager;

import java.io.File;

/**
 * Created by rls on 26/07/14.
 */
public class SexyTopoActivity extends ActionBarActivity {

    protected SurveyManager dataManager;

    public SexyTopoActivity() {
        super();

        dataManager = SurveyManager.getInstance(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

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
            case R.id.action_settings:
                startActivity(SettingsActivity.class);
                return true;


            case R.id.action_file_new:
                startNewSurvey();
                return true;
            case R.id.action_file_open:
                openSurvey();
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
            case R.id.action_file_export:
                //exportSurvey();
                return true;



            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private void startActivity(Class clazz) {
        if (! clazz.isInstance(this)) {
            Intent intent = new Intent(this, clazz);
            startActivity(intent);
        }
    }

    private void saveSurvey() {
        try {
            Saver.save(this, getSurvey());
        } catch (Exception e) {
            Log.d(SexyTopo.TAG, "Error saving survey: " + e);
            showSimpleToast("Error saving survey");
        }
    }


    private void saveSurveyAsName() {

        final EditText input = new EditText(this);
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
                            Saver.save(SexyTopoActivity.this, survey);
                        } catch (Exception e) {
                            survey.setName(oldName);
                            showSimpleToast("Error saving survey with new name");
                            Log.d(SexyTopo.TAG, "Error saving survey: " + e);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }


    private void startNewSurvey() {

        final EditText input = new EditText(this);
        input.setText("NewSurvey");

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_new_survey_title))
                .setMessage(getString(R.string.field_survey_name))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        String name = value.toString();
                        Survey survey = new Survey(name);
                        setSurvey(survey);
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
    }

    private void deleteSurvey() {

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_survey_title))
                .setMessage(getString(R.string.dialog_delete_survey_content))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            String surveyName = getSurvey().getName();
                            Util.deleteSurvey(surveyName);
                        } catch (Exception e) {
                            showSimpleToast("Error deleting survey");
                            Log.d(SexyTopo.TAG, "Error deleting survey: " + e);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }


    private void openSurvey() {

        File[] surveyDirectories = Util.getSurveyDirectories();

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                 this);
        //builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle("Open Survey");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.select_dialog_item);

        for (File file : surveyDirectories) {
            arrayAdapter.add(file.getName());
        }
        /*
        arrayAdapter.add("Hardik");
        arrayAdapter.add("Archit");
        arrayAdapter.add("Jignesh");
        arrayAdapter.add("Umang");
        arrayAdapter.add("Ogof Ffynnon Ddu");
        arrayAdapter.add("Hardik");
        arrayAdapter.add("Archit");
        arrayAdapter.add("Jignesh");
        arrayAdapter.add("Umang");
        arrayAdapter.add("Gatti");*/
        builderSingle.setNegativeButton("Cancel",
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
                        String strName = arrayAdapter.getItem(which);
                        AlertDialog.Builder builderInner = new AlertDialog.Builder(
                                SexyTopoActivity.this);
                        builderInner.setMessage(strName);
                        builderInner.setTitle("Your Selected Item is");
                        builderInner.setPositiveButton("Ok",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {
                                        dialog.dismiss();
                                    }
                                });
                        builderInner.show();
                    }
                });
        builderSingle.show();
    }


    protected Survey getSurvey() {
        return SurveyManager.getInstance(this).getCurrentSurvey();
    }

    protected void setSurvey(Survey survey) {
        SurveyManager.getInstance(this).setCurrentSurvey(survey);
    }


    protected void showSimpleToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

}
