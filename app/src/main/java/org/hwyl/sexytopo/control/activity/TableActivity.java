package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.table.ManualEntry;
import org.hwyl.sexytopo.control.table.TableRowAdapter;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
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
import java.util.List;


public class TableActivity extends SexyTopoActivity
    implements PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener, TableRowAdapter.OnRowClickListener {

    private final GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    private RecyclerView recyclerView;
    private TableRowAdapter tableRowAdapter;
    private BroadcastReceiver receiver;
    private TextView cellBeingClicked;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);
        setupMaterialToolbar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        recyclerView = findViewById(R.id.tableRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tableRowAdapter = new TableRowAdapter(this, getSurvey(), this);
        recyclerView.setAdapter(tableRowAdapter);

        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                syncTableWithSurvey();
            }
        };
    }

    private void measureAndSyncHeaderWidths() {
        TableLayout headerTable = findViewById(R.id.HeaderTable);
        List<Integer> widths = new ArrayList<>();

        if (headerTable.getChildCount() > 0) {
            android.widget.TableRow headerRow = (android.widget.TableRow) headerTable.getChildAt(0);
            for (int i = 0; i < headerRow.getChildCount(); i++) {
                View cell = headerRow.getChildAt(i);
                widths.add(cell.getWidth());
            }
            tableRowAdapter.setColumnWidths(widths);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver, new IntentFilter(SexyTopoConstants.SURVEY_UPDATED_EVENT));

        syncTableWithSurvey();

        // Measure header widths after layout is complete
        TableLayout headerTable = findViewById(R.id.HeaderTable);
        headerTable.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                headerTable.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                measureAndSyncHeaderWidths();
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getString(SexyTopoConstants.JUMP_TO_STATION) != null) {
            String requestedStationName = bundle.getString(SexyTopoConstants.JUMP_TO_STATION);
            Station requestedStation = getSurvey().getStationByName(requestedStationName);
            jumpToStation(requestedStation);
        } else {
            // Scroll to bottom (latest data)
            int itemCount = tableRowAdapter.getItemCount();
            if (itemCount > 0) {
                recyclerView.scrollToPosition(itemCount - 1);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(receiver);
    }


    private void jumpToStation(Station station) {
        try {
            int position = tableRowAdapter.getPositionForStation(station);
            if (position >= 0) {
                // Post to ensure RecyclerView layout is complete
                recyclerView.post(() -> {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        // Scroll so target is at the top of the visible area (with small offset for header)
                        layoutManager.scrollToPositionWithOffset(position, 0);
                    }
                });
            }
        } catch (Exception exception) {
            String name = station == null? getString(R.string.unknown) : station.getName();
            showExceptionAndLog(R.string.context_jump_to_station_error, exception, name);
        }
    }


    public void syncTableWithSurvey() {
        Survey survey = getSurvey();
        tableRowAdapter.setSurvey(survey);

        List<GraphToListTranslator.SurveyListEntry> tableEntries =
                graphToListTranslator.toChronoListOfSurveyListEntries(survey);

        if (tableEntries.isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.no_data,
                    Toast.LENGTH_SHORT).show();
        }

        tableRowAdapter.setEntries(tableEntries);
    }

    @Override
    public void onRowLongClick(View view, GraphToListTranslator.SurveyListEntry entry, TableCol col) {
        TextView textView = (TextView) view;
        cellBeingClicked = textView;

        if (col == TableCol.FROM || col == TableCol.TO) {
            showPopup(view, R.menu.table_station_selected, this);
        } else {
            Leg leg = entry.getLeg();
            if (leg.hasDestination()) {
                showPopup(view, R.menu.table_full_leg_selected, this);
            } else {
                showPopup(view, R.menu.table_splay_selected, this);
            }
        }
    }


    @SuppressWarnings("UnusedParameters")
    private void showPopup(View view, int id, PopupMenu.OnMenuItemClickListener listener) {

        TextView selectedCell = (TextView)view;

        selectedCell.setTextColor(ContextCompat.getColor(this, R.color.red));

        PopupMenu popup = new PopupMenu(this, selectedCell);
        popup.getMenuInflater().inflate(id, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.setOnDismissListener(this);
        popup.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        final TableCol col = tableRowAdapter.getFieldToTableCol().get(cellBeingClicked);
        final GraphToListTranslator.SurveyListEntry surveyEntry =
                tableRowAdapter.getFieldToSurveyEntry().get(cellBeingClicked);

        if (col == null || surveyEntry == null) {
            return false;
        }

        Context context = this;
        int itemId = menuItem.getItemId();

        if (itemId == R.id.setActiveStation) {
            Station newActive = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
            if (newActive == Survey.NULL_STATION) {
                showSimpleToast(R.string.context_set_active_splay_error);
            } else {
                getSurvey().setActiveStation(newActive);
                syncTableWithSurvey();
            }
            return true;
        } else if (itemId == R.id.graph_station_jump_to_plan) {
            Station planStation = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
            if (planStation == Survey.NULL_STATION) {
                showSimpleToast(R.string.context_jump_to_splay_error);
            } else {
                jumpToStation(planStation, PlanActivity.class);
            }
            return true;
        } else if (itemId == R.id.graph_station_jump_to_ee) {
            Station eeStation = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
            if (eeStation == Survey.NULL_STATION) {
                showSimpleToast(R.string.context_jump_to_splay_error);
            } else {
                jumpToStation(eeStation, ExtendedElevationActivity.class);
            }
            return true;
        } else if (itemId == R.id.renameStation) {
            Station toRename = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
            if (toRename == Survey.NULL_STATION) {
                showSimpleToast(R.string.context_rename_splay_error);
            } else {
                ManualEntry.renameStation(this, getSurvey(), toRename);
            }
            return true;
        } else if (itemId == R.id.editLeg) {
            Station fromStation = surveyEntry.getFrom();
            Leg toEdit = surveyEntry.getLeg();
            ManualEntry.editLeg(this, getSurvey(), fromStation, toEdit);
            return true;
        } else if (itemId == R.id.moveRow) {
            final Leg toMove = surveyEntry.getLeg();
            requestMoveLeg(toMove);
            return true;
        } else if (itemId == R.id.upgradeRow) {
            Leg toUpgrade = surveyEntry.getLeg();
            SurveyUpdater.upgradeSplayToConnectedLeg(getSurvey(), toUpgrade, getInputMode());
            syncTableWithSurvey();
            return true;
        } else if (itemId == R.id.deleteStation) {
            Station toDelete = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
            askAboutDeleting(context, toDelete, null);
            return true;
        } else if (itemId == R.id.deleteLeg) {
            askAboutDeleting(context, surveyEntry.getFrom(), surveyEntry.getLeg());
            return true;
        } else if (itemId == R.id.deleteSplay) {
            askAboutDeleting(context, surveyEntry.getFrom(), surveyEntry.getLeg());
            return true;
        } else {
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

        String message = context.getString(R.string.context_this_will_delete);

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

        new MaterialAlertDialogBuilder(context)
            .setMessage(message)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                Survey survey = getSurvey();
                if (deletingLeg) {
                    SurveyUpdater.deleteSplay(survey, station, leg);
                } else {
                    SurveyUpdater.deleteStation(survey, station);
                }
                getSurveyManager().broadcastSurveyUpdated();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }


    @Override
    public void onDismiss(PopupMenu popupMenu) {
        int usualTextColour = ContextCompat.getColor(this, R.color.bodyTextColor);
        cellBeingClicked.setTextColor(usualTextColour);
    }


    public void manuallyAddStation(View view) {
        if (GeneralPreferences.isManualLrudModeOn()) {
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
            showSimpleToast(R.string.context_move_leg_no_valid_move);
            return;
        }

        // Reverse the order of stations to show the most recent stations at the top
        java.util.Collections.reverse(stations);
        
        for (Station station : stations) {
            spinnerArray.add(station.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner spinner = stationView.findViewById(R.id.stationSpinner);
        spinner.setAdapter(adapter);

        new MaterialAlertDialogBuilder(this)
            .setMessage(R.string.context_move_leg_select_station_title)
            .setView(stationView)
            .setPositiveButton(R.string.move, (dialog, which) -> {
                String selectedName = spinner.getSelectedItem().toString();
                Station newStation = getSurvey().getStationByName(selectedName);

                SurveyUpdater.moveLeg(getSurvey(), toMove, newStation);
                syncTableWithSurvey();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }


}
