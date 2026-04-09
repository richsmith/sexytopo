package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TableLayout;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.graph.ContextMenuManager;
import org.hwyl.sexytopo.control.graph.ViewContext;
import org.hwyl.sexytopo.control.table.LegDialogs;
import org.hwyl.sexytopo.control.table.TableRowAdapter;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.control.util.LegMover;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.ArrayList;
import java.util.List;


public class TableActivity extends SurveyEditorActivity
    implements TableRowAdapter.OnRowClickListener {

    private final GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    private RecyclerView recyclerView;
    private TableRowAdapter tableRowAdapter;
    private BroadcastReceiver receiver;
    private ContextMenuManager contextMenuManager;
    private View highlightedRow;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);
        setupMaterialToolbar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        contextMenuManager = new ContextMenuManager(this, ViewContext.TABLE, this);

        recyclerView = findViewById(R.id.tableRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tableRowAdapter = new TableRowAdapter(this, getSurvey(), this);
        recyclerView.setAdapter(tableRowAdapter);

        // Set up FAB click listeners
        findViewById(R.id.fabAddStation).setOnClickListener(v -> manuallyAddStation());
        findViewById(R.id.fabAddSplay).setOnClickListener(v -> manuallyAddSplay());

        // Apply edge-to-edge insets
        setupEdgeToEdge();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                syncWithSurvey();
            }
        };
    }

    private void setupEdgeToEdge() {
        View rootLayout = findViewById(R.id.rootLayout);
        FloatingActionButton fabAddStation = findViewById(R.id.fabAddStation);
        FloatingActionButton fabAddSplay = findViewById(R.id.fabAddSplay);
        RecyclerView recyclerView = findViewById(R.id.tableRecyclerView);

        int fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        int fabVerticalSpacing = getResources().getDimensionPixelSize(R.dimen.fab_vertical_spacing);

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

            if (isPortrait) {
                // Portrait: let content slide behind nav bar, but keep FABs clear
                rootLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

                // Add bottom padding to RecyclerView so content can scroll clear of FABs
                recyclerView.setPadding(0, 0, 0, systemBars.bottom);
                recyclerView.setClipToPadding(false);
            } else {
                // Landscape: apply all insets as padding to keep everything clear
                rootLayout.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                );
                recyclerView.setPadding(0, 0, 0, 0);
            }

            // Position FABs with margins relative to the padded area
            fabAddStation.post(() -> {
                int normalFabSize = fabAddStation.getHeight();

                CoordinatorLayout.LayoutParams stationParams =
                    (CoordinatorLayout.LayoutParams) fabAddStation.getLayoutParams();
                CoordinatorLayout.LayoutParams splayParams =
                    (CoordinatorLayout.LayoutParams) fabAddSplay.getLayoutParams();

                if (isPortrait) {
                    stationParams.bottomMargin = systemBars.bottom + fabMargin;
                    stationParams.rightMargin = fabMargin;

                    splayParams.bottomMargin = systemBars.bottom + fabMargin + normalFabSize + fabVerticalSpacing;
                    splayParams.rightMargin = fabMargin;
                } else {
                    stationParams.bottomMargin = fabMargin;
                    stationParams.rightMargin = fabMargin;

                    splayParams.bottomMargin = fabMargin + normalFabSize + fabVerticalSpacing;
                    splayParams.rightMargin = fabMargin;
                }

                fabAddStation.setLayoutParams(stationParams);
                fabAddSplay.setLayoutParams(splayParams);
            });

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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

        syncWithSurvey();

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


    @Override
    public void syncWithSurvey() {
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
    public void onRowClick(View view, GraphToListTranslator.SurveyListEntry entry, TableCol col) {
        Station fromStation = entry.getFrom();
        Leg leg = entry.getLeg();
        LegDialogs.editLeg(this, getSurvey(), fromStation, leg);
    }

    @Override
    public void onRowLongClick(View view, GraphToListTranslator.SurveyListEntry entry, TableCol col) {
        Leg leg = entry.getLeg();
        Station fromStation = entry.getFrom();

        // Determine which station was tapped, accounting for backward shots
        Station station;
        boolean isStationColumn = (col == TableCol.FROM || col == TableCol.TO);
        boolean tapOnFromStation = (col == TableCol.FROM) ^ leg.wasShotBackwards();
        if (leg.hasDestination()) {
            station = tapOnFromStation ? fromStation : leg.getDestination();
        } else {
            station = fromStation;
        }

        // Highlight the entire row using ViewHolder
        RecyclerView.ViewHolder viewHolder = recyclerView.findContainingViewHolder(view);
        if (viewHolder != null) {
            clearHighlight();
            highlightedRow = viewHolder.itemView;

            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;

            ColorDrawable highlightDrawable = new ColorDrawable(primaryColor);
            highlightDrawable.setAlpha(51);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                highlightedRow.setForeground(highlightDrawable);
            } else {
                highlightedRow.setBackgroundColor(primaryColor & 0x33FFFFFF);
            }
        }

        if (isStationColumn) {
            contextMenuManager.showMenuForStation(view, station, getSurvey(), this::clearHighlight);
        } else {
            String customTitle;
            if (leg.hasDestination()) {
                String from = leg.wasShotBackwards() ? leg.getDestination().getName() : fromStation.getName();
                String to = leg.wasShotBackwards() ? fromStation.getName() : leg.getDestination().getName();
                customTitle = getString(R.string.menu_context_title_leg, from, to);
            } else {
                customTitle = getString(R.string.menu_context_title_splay, fromStation.getName());
            }
            contextMenuManager.showMenuForLeg(view, station, getSurvey(), customTitle, this::clearHighlight, leg);
        }
    }

    private void clearHighlight() {
        if (highlightedRow != null) {
            // Clear foreground highlight
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                highlightedRow.setForeground(null);
            } else {
                highlightedRow.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(this, android.R.color.transparent));
            }
            highlightedRow = null;
        }
    }


    @Override
    protected void invalidateView() {
        syncWithSurvey();
    }

    @Override
    public void onRenameStation(Station station) {
        LegDialogs.renameStation(this, getSurvey(), station);
    }




    private void manuallyAddStation() {
        if (GeneralPreferences.isManualLrudModeOn()) {
            LegDialogs.addStationWithLruds(this, getSurvey());
        } else {
            LegDialogs.addStation(this, getSurvey());
        }
    }

    private void manuallyAddSplay() {
        LegDialogs.addSplay(this, getSurvey());
    }

    public void deleteLastLeg(View view) {
        getSurvey().undoAddLeg();
        syncWithSurvey();
    }

}
