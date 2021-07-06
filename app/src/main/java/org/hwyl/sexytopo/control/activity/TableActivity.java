package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.graph.GraphView;
import org.hwyl.sexytopo.control.table.ManualEntry;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.control.util.LegMover;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class TableActivity extends SexyTopoActivity
        implements
            PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnDismissListener,
            OnLongClickListener {

    private final GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    private final Map<TextView, GraphToListTranslator.SurveyListEntry> fieldToSurveyEntry
            = new HashMap<>();
    private final Map<TextView, TableCol> fieldToTableCol = new HashMap<>();
    private final static Map<Station, Integer> stationsToTableIndex = new HashMap<>();
    private TextView cellBeingClicked;


    private static final EnumMap<TableCol, Integer> TABLE_COL_BY_ANDROID_ID =
        new EnumMap<TableCol, Integer>(TableCol.class) {{
            put(TableCol.FROM, R.id.tableRowFrom);
            put(TableCol.TO, R.id.tableRowTo);
            put(TableCol.DISTANCE, R.id.tableRowDistance);
            put(TableCol.AZIMUTH, R.id.tableRowAzimuth);
            put(TableCol.INCLINATION, R.id.tableRowInclination);

        }};


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                syncTableWithSurvey();
            }
        };
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver, new IntentFilter(SexyTopo.SURVEY_UPDATED_EVENT));
    }


    @Override
    protected void onResume() {
        super.onResume();

        syncTableWithSurvey();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getString(SexyTopo.JUMP_TO_STATION) != null) {
            String requestedStationName = bundle.getString(SexyTopo.JUMP_TO_STATION);
            Station requestedStation = getSurvey().getStationByName(requestedStationName);
            jumpToStation(requestedStation);
        } else {
            final ScrollView scrollView = findViewById(R.id.BodyTableScrollView);
            scrollView.fullScroll(View.FOCUS_DOWN);
        }
    }


    private void jumpToStation(Station station) {
        try {
            final TableLayout tableLayout = findViewById(R.id.BodyTable);
            int requestedIndex = stationsToTableIndex.get(station);
            final View requestedRow = tableLayout.getChildAt(requestedIndex);
            final ScrollView scrollView = findViewById(R.id.BodyTableScrollView);

            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.smoothScrollTo(0, requestedRow.getTop());
                }
            });

        } catch (Exception exception) {
            String name = station == null? "Unknown" : station.getName();
            Log.e("Could not jump to station " + name);
            Log.e(exception);
        }
    }


    public void syncTableWithSurvey() {

        if (!isInForeground()) {
            return;
        }

        Survey survey = getSurvey();

        stationsToTableIndex.clear();

        List<GraphToListTranslator.SurveyListEntry> tableEntries =
                graphToListTranslator.toChronoListOfSurveyListEntries(survey);

        if (tableEntries.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.no_data,
                    Toast.LENGTH_SHORT).show();
        }

        final TableLayout tableLayout = findViewById(R.id.BodyTable);
        tableLayout.removeAllViews();

        for (GraphToListTranslator.SurveyListEntry entry : tableEntries) {

            TableRow tableRow = (TableRow)LayoutInflater.from(this).inflate(R.layout.table_row, null);
            final Map<TableCol, Object> map = GraphToListTranslator.createMap(entry);

            for (TableCol col : TableCol.values()) {

                if (col == TableCol.COMMENT) {
                    continue;
                }

                String display = map.containsKey(col) ? col.format(map.get(col)) : "?";
                int id = TABLE_COL_BY_ANDROID_ID.get(col);
                TextView textView = tableRow.findViewById(id);
                textView.setText(display);

                if (isActiveStation(map.get(col))) {
                    textView.setBackgroundColor(GraphView.HIGHLIGHT_COLOUR.intValue);
                }

				if (entry.getLeg().hasDestination()) {
					textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                } else {
                    textView.setTypeface(textView.getTypeface(), Typeface.NORMAL);
                }

                fieldToSurveyEntry.put(textView, entry);
                fieldToTableCol.put(textView, col);

                textView.setOnLongClickListener(this);
            }

            int rowCount = tableLayout.getChildCount();
            tableLayout.addView(tableRow, rowCount);

            if (entry.getLeg().hasDestination()) {
                Station to = entry.getLeg().getDestination();
                stationsToTableIndex.put(to, rowCount);
            }
        }

        tableLayout.requestLayout();


    }

    private boolean isActiveStation(Object object) {
        return (object instanceof Station) &&
            object == getSurvey().getActiveStation();
    }

    @Override
    public boolean onLongClick(View view) {
        TextView textView = (TextView)view;
        cellBeingClicked = textView;
        TableCol col = fieldToTableCol.get(textView);

        if (col == TableCol.FROM || col == TableCol.TO) {
            showPopup(view, R.menu.table_station_selected, this);
        } else {
            final GraphToListTranslator.SurveyListEntry surveyEntry =
                    fieldToSurveyEntry.get(cellBeingClicked);
            Leg leg = surveyEntry.getLeg();
            if (leg.hasDestination()) {
                showPopup(view, R.menu.table_full_leg_selected, this);
            } else {
                showPopup(view, R.menu.table_splay_selected, this);
            }
        }

        return true;
    }


    @SuppressWarnings("UnusedParameters")
    private void showPopup(View view, int id, PopupMenu.OnMenuItemClickListener listener) {

        TextView selectedCell = (TextView)view;

        selectedCell.setTextColor(Color.RED);

        PopupMenu popup = new PopupMenu(this, selectedCell);
        popup.getMenuInflater().inflate(id, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.setOnDismissListener(this);
        popup.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        final TableCol col = fieldToTableCol.get(cellBeingClicked);
        final GraphToListTranslator.SurveyListEntry surveyEntry =
                fieldToSurveyEntry.get(cellBeingClicked);


        Context context = this;

        switch (menuItem.getItemId()) {

            case R.id.setActiveStation:
                Station newActive = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
                getSurvey().setActiveStation(newActive);
                syncTableWithSurvey();
                return true;

            case R.id.graph_station_jump_to_plan:
                Station planStation = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
                jumpToStation(planStation, PlanActivity.class);
                return true;

            case R.id.graph_station_jump_to_ee:
                Station eeStation = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
                jumpToStation(eeStation, ExtendedElevationActivity.class);
                return true;

            case R.id.renameStation:
                Station toRename = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
                if (toRename == Survey.NULL_STATION) {
                    showSimpleToast("Can't rename a splay end");
                } else {
                    ManualEntry.renameStation(this, getSurvey(), toRename);
                }
                return true;

            case R.id.editLeg:
                Leg toEdit = surveyEntry.getLeg();
                ManualEntry.editLeg(this, getSurvey(), toEdit);
                return true;

            case R.id.moveRow:
                final Leg toMove = surveyEntry.getLeg();
                requestMoveLeg(toMove);
                return true;

            case R.id.upgradeRow:
                Leg toUpgrade = surveyEntry.getLeg();
                SurveyUpdater.upgradeSplayToConnectedLeg(getSurvey(), toUpgrade, getInputMode());
                syncTableWithSurvey();
                return true;

            case R.id.deleteStation:
                Station toDelete = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
                askAboutDeleting(context, toDelete, null);
                return true;

            case R.id.deleteLeg:
                askAboutDeleting(context, surveyEntry.getFrom(), surveyEntry.getLeg());
                return true;

            case R.id.deleteSplay:
                askAboutDeleting(context, surveyEntry.getFrom(), surveyEntry.getLeg());
                return true;

            default:
                return false;
        }

    }



    /*
        This method is a bit convoluted but reflects the UI choices.

        We are either deleting a splay or a leg (if we are deleting a station,
        we still have to delete the source leg). If the latter, we are also deleting any
        onward survey from the station.

        If leg is null, we are deleting the station
        If leg is not null, we are only deleting the leg from the specified station
     */
    public void askAboutDeleting(Context context, final Station station, final Leg leg) {

        int numFullLegsToBeDeleted = 0;
        int numSplaysToBeDeleted = 0;

        final boolean deletingLeg = leg != null;
        boolean deletingSplay = (deletingLeg && !leg.hasDestination());

        if (deletingSplay) {
            numSplaysToBeDeleted += 1;

        } else {
            numFullLegsToBeDeleted++;
            Station root = deletingLeg? leg.getDestination() : station;
            numFullLegsToBeDeleted += SurveyStats.calcNumberSubFullLegs(root);
            numSplaysToBeDeleted += SurveyStats.calcNumberSubSplays(root);
        }

        String message = context.getString(R.string.this_will_delete);

        if (numFullLegsToBeDeleted > 0) {
            String noun = context.getString(R.string.leg).toLowerCase();
            message += "\n" + TextTools.pluralise(numFullLegsToBeDeleted, noun);
            noun = context.getString(R.string.station).toLowerCase();
            message += " (" + TextTools.pluralise(numFullLegsToBeDeleted, noun) + ")";
        }
        if (numSplaysToBeDeleted > 0) {
            String noun = context.getString(R.string.splay).toLowerCase();
            message += "\n" + TextTools.pluralise(numSplaysToBeDeleted, noun);
        }

        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Survey survey = getSurvey();
                        if (deletingLeg) {
                            SurveyUpdater.deleteSplay(survey, station, leg);
                        } else {
                            SurveyUpdater.deleteStation(survey, station);
                        }
                        getSurveyManager().broadcastSurveyUpdated();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    @Override
    public void onDismiss(PopupMenu popupMenu) {
        // Ideally this colour should be named somewhere, but for now...
        int usualTextColour = Color.rgb(0x7F, 0x7F, 0x7F);
        cellBeingClicked.setTextColor(usualTextColour);
    }


    public void manuallyAddStation(View view) {
        if (getBooleanPreference("pref_key_lrud_fields")) {
            ManualEntry.addStationWithLruds(this, getSurvey());
        } else {
            ManualEntry.addStation(this, getSurvey());
        }
    }


    public void manuallyAddSplay(View view) {
        ManualEntry.addSplay(this, getSurvey());
    }

    public void deleteLastLeg(View view) {
        getSurvey().undoAddLeg();
        syncTableWithSurvey();
    }


    @SuppressLint("InflateParams")
    private void requestMoveLeg(final Leg toMove) {
        View stationView = getLayoutInflater().inflate(R.layout.select_station_dialog, null);

        List<String> spinnerArray =  new ArrayList<>();
        List<Station> stations = LegMover.getValidDestinations(getSurvey(), toMove);

        if (stations.isEmpty()) {
            showSimpleToast(R.string.move_leg_no_valid_move);
            return;
        }

        for (Station station : stations) {
            spinnerArray.add(station.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner spinner = stationView.findViewById(R.id.stationSpinner);
        spinner.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setMessage(R.string.move_leg_select_station_title)
                .setView(stationView)
                .setPositiveButton(R.string.move, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedName = spinner.getSelectedItem().toString();
                        Station newStation = getSurvey().getStationByName(selectedName);

                        SurveyUpdater.moveLeg(getSurvey(), toMove, newStation);
                        syncTableWithSurvey();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


}
