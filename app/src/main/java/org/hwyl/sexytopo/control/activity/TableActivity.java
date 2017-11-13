package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.graph.GraphView;
import org.hwyl.sexytopo.control.table.ManualEntry;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableActivity extends SexyTopoActivity
        implements
            PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnDismissListener,
            OnLongClickListener {

    private GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    private Map<TextView, GraphToListTranslator.SurveyListEntry> fieldToSurveyEntry = new HashMap<>();
    private Map<TextView, TableCol> fieldToTableCol = new HashMap<>();

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
    protected void onStart() {
        super.onStart();
        syncTableWithSurvey();
    }


    @Override
    protected void onResume() {
        super.onResume();
        syncTableWithSurvey();
    }


    public void syncTableWithSurvey() {

        Survey survey = getSurvey();

        List<GraphToListTranslator.SurveyListEntry> tableEntries =
                graphToListTranslator.toListOfSurveyListEntries(survey);

        if (tableEntries.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.no_data,
                    Toast.LENGTH_SHORT).show();
        }

        TableLayout tableLayout = (TableLayout)(findViewById(R.id.BodyTable));
        tableLayout.removeAllViews();

        for (GraphToListTranslator.SurveyListEntry entry : tableEntries) {

            TableRow tableRow = (TableRow)LayoutInflater.from(this).inflate(R.layout.table_row, null);
            final Map map = GraphToListTranslator.createMap(entry);

            for (TableCol col : TableCol.values()) {

                if (col == TableCol.COMMENT) {
                    continue;
                }

                String display = map.containsKey(col) ? col.format(map.get(col)) : "?";
                int id = TABLE_COL_BY_ANDROID_ID.get(col);
                TextView textView = (TextView) tableRow.findViewById(id);
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

            int numRows = tableLayout.getChildCount();
            tableLayout.addView(tableRow, numRows);
        }

        tableLayout.requestLayout();

        ScrollView scrollView = (ScrollView)(findViewById(R.id.BodyTableScrollView));
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    private boolean isActiveStation(Object object) {
        return (object instanceof Station) &&
                ((Station)object) == getSurvey().getActiveStation();
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

        switch (menuItem.getItemId()) {

            case R.id.setActiveStation:
                Station newActive = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
                getSurvey().setActiveStation(newActive);
                syncTableWithSurvey();
                return true;
            case R.id.renameStation:
                Station toRename = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
                ManualEntry.renameStation(this, getSurvey(), toRename);
                return true;
            case R.id.editLeg:
                Leg toEdit = surveyEntry.getLeg();
                ManualEntry.editLeg(this, getSurvey(), toEdit);
                return true;
            case R.id.upgradeRow:
                Leg toUpgrade = surveyEntry.getLeg();
                SurveyUpdater.upgradeSplayToConnectedLeg(getSurvey(), toUpgrade);
                syncTableWithSurvey();
                return true;
            case R.id.deleteRow:
                int legsToBeDeleted = SurveyStats.calcNumberSubLegs(surveyEntry.getFrom());
                int stationsToBeDeleted = SurveyStats.calcNumberSubStations(surveyEntry.getFrom());
                String detailMessage = "This will delete\n" +
                        TextTools.pluralise(legsToBeDeleted, "leg") +
                        " and " + TextTools.pluralise(stationsToBeDeleted, "station");
                String message = (legsToBeDeleted > 1 || stationsToBeDeleted > 1)?
                        detailMessage : getString(R.string.delete_row_question);
                new AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SurveyUpdater.deleteLeg(getSurvey(), surveyEntry.getLeg());
                            syncTableWithSurvey();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
                return true;
            default:
                return false;
        }

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
        getSurvey().undoLeg();
        syncTableWithSurvey();
    }


}
