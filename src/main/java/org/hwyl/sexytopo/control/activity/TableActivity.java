package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;
import org.hwyl.sexytopo.util.GraphToListTranslator;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableActivity extends SexyTopoActivity
        implements PopupMenu.OnMenuItemClickListener, OnLongClickListener {

    private GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    private Map<TextView, GraphToListTranslator.SurveyListEntry> fieldToSurveyEntry = new HashMap<>();
    private Map<TextView, TableCol> fieldToTableCol = new HashMap<>();


    private static final EnumMap<TableCol, Integer> TABLE_COL_BY_ANDROID_ID =
        new EnumMap<TableCol, Integer>(TableCol.class) {{
            put(TableCol.FROM, R.id.tableRowFrom);
            put(TableCol.TO, R.id.tableRowTo);
            put(TableCol.DISTANCE, R.id.tableRowDistance);
            put(TableCol.BEARING, R.id.tableRowBearing);
            put(TableCol.INCLINATION, R.id.tableRowInclination);

        }};



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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



    private void syncTableWithSurvey() {

        Survey survey = getSurvey();

        List<GraphToListTranslator.SurveyListEntry> tableEntries =
                graphToListTranslator.toListOfSurveyListEntries(survey);

        if (tableEntries.size() == 0) {
            Toast.makeText(getApplicationContext(), "No data",
                    Toast.LENGTH_SHORT).show();
        }

        TableLayout tableLayout = (TableLayout)(findViewById(R.id.BodyTable));
        tableLayout.removeAllViews();


        // Beware: the following code is pretty hideous!
        //
        // I suspect it would be better to display the data in a tree structure, but that looks
        // very difficult to do in Android at present so we'll stick to one big list.
        //
        // Essentially we are creating a list and creating a new listener for every field.
        // This may cause performance issues for large surveys... let's deal with that when it
        // happens.
        //
        // Each listener is created referencing the data it is able to operate on... it's like a
        // pseudo-closure...

        for (GraphToListTranslator.SurveyListEntry entry : tableEntries) {

            TableRow tableRow = (TableRow)LayoutInflater.from(this).inflate(R.layout.table_row, null);
            final Map map = GraphToListTranslator.createMap(entry);

            for (TableCol col : TableCol.values()) {

                String display = map.containsKey(col) ? col.format(map.get(col)) : "?";
                int id = TABLE_COL_BY_ANDROID_ID.get(col);
                TextView textView = (TextView) tableRow.findViewById(id);
                textView.setText(display);


                fieldToSurveyEntry.put(textView, entry);
                fieldToTableCol.put(textView, col);

                //textView.setOnLongClickListener(onLongClickTableRow);


                //final Station station = (Station) (map.get(col));


                textView.setOnLongClickListener(this);
            }

/*
                textView.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(final View view) {
                        PopupMenu.OnMenuItemClickListener listener =
                            new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    view.setBackgroundColor(Color.GRAY);

                                    switch (menuItem.getItemId()) {
                                        case R.id.setActiveStation:
                                            final Station station = (Station) (map.get(col));
                                            clickForStation(station);
                                            return true;
                                        case R.id.editLeg:
                                            final Leg leg = entry.getLeg();
                                            //clickForLeg(leg);
                                            return true;
                                        default:
                                            return false;
                                    }

                                }
                            };

                        if (col == TableCol.FROM || col == TableCol.TO) {
                            showPopup(view, R.menu.table_station_selected, listener);
                        } else {
                            showPopup(view, R.menu.table_station_selected, listener);
                            //showPopup(view, R.menu.table_leg_selected, listener);
                        }
                        return true;
                    }
                });
            }*/

            int numRows = tableLayout.getChildCount();
            tableLayout.addView(tableRow, numRows);
        }

        tableLayout.requestLayout();

        ScrollView scrollView = (ScrollView)(findViewById(R.id.BodyTableScrollView));
        scrollView.fullScroll(View.FOCUS_DOWN);
    }


    //@Override
    public boolean onLongClick(View view) {
        TextView textView = (TextView)view;
        cellBeingClicked = textView;
        TableCol col = fieldToTableCol.get(textView);
        GraphToListTranslator.SurveyListEntry surveyListEntry = fieldToSurveyEntry.get(textView);

        if (col == TableCol.FROM || col == TableCol.TO) {
            showPopup(view, R.menu.table_station_selected, this);
        } else {
            // show slightly different popup
        }

        return true;
    }

/*
    private void syncTableWithSurvey2() {

        Survey survey = getSurvey();

        List<Map<TableCol, Object>> tableEntries = graphToListTranslator.toListOfSurveyListEntries(survey);

        if (tableEntries.size() == 0) {
            Toast.makeText(getApplicationContext(), "No data",
                    Toast.LENGTH_SHORT).show();
        }

        TableLayout tableLayout = (TableLayout)(findViewById(R.id.BodyTable));
        tableLayout.removeAllViews();

        for (Map<TableCol, Object> map : tableEntries) {

            TableRow tableRow = (TableRow)LayoutInflater.from(this).inflate(R.layout.table_row, null);



            for (TableCol col : TableCol.values()) {

                String display = map.containsKey(col)? col.format(map.get(col)) : "?";
                int id = TABLE_COL_BY_ANDROID_ID.get(col);
                TextView textView = (TextView)tableRow.findViewById(id);
                textView.setText(display);

                //textView.setOnLongClickListener(onLongClickTableRow);

                if (col == TableCol.FROM || col == TableCol.TO) {
                    final Station station = (Station)(map.get(col));
                    textView.setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(final View view) {
                            PopupMenu.OnMenuItemClickListener listener =
                                    new PopupMenu.OnMenuItemClickListener() {
                                        @Override
                                        public boolean onMenuItemClick(MenuItem menuItem) {
                                            view.setBackgroundColor(Color.GRAY);
                                            clickForStation(station);
                                            return true;
                                        }
                                    };
                            showPopup(view, R.menu.table_station_selected, listener);
                            return true;
                        }
                    });
                }
            }

            int numRows = tableLayout.getChildCount();
            tableLayout.addView(tableRow, numRows);
        }

        tableLayout.requestLayout();

        ScrollView scrollView = (ScrollView)(findViewById(R.id.BodyTableScrollView));
        scrollView.fullScroll(View.FOCUS_DOWN);
    }*/

    private void clickForStation(Station station) {
        getSurvey().setActiveStation(station);
    }

    private void clickForLeg(Leg leg) {
        int ignore = 0;
    }

    private void handlePopupClick() {
    }

    private void showPopup(View view, int id, PopupMenu.OnMenuItemClickListener listener) {

        TextView selectedCell = (TextView)view;

        selectedCell.setBackgroundColor(Color.WHITE);

        PopupMenu popup = new PopupMenu(getBaseContext(), selectedCell);
        popup.getMenuInflater().inflate(id, popup.getMenu());
        popup.setOnMenuItemClickListener(listener);
        popup.show();
    }

    private TextView cellBeingClicked;

    private View.OnLongClickListener onLongClickTableRow = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View view) {
            cellBeingClicked = (TextView)view;

            cellBeingClicked.setBackgroundColor(Color.WHITE);

            PopupMenu popup = new PopupMenu(getBaseContext(), cellBeingClicked);
            popup.getMenuInflater().inflate(R.menu.table_station_selected, popup.getMenu());
            popup.setOnMenuItemClickListener(TableActivity.this);
            popup.show();
            //break;

            /*
            if(AlreadySelctedRow >= 0){
                SelectedRow = (TableRow) findViewById(AlreadySelctedRow);
                SelectedRow.setBackgroundColor(0xFFCCD0);
            }
            SelectedRow = (TableRow)v;

            SelectedRow.setBackgroundColor(Color.WHITE);
            AlreadySelctedRow = v.getId();
            showPopupMenu(v);
            return false;*/
            return true;
        }
    };

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        cellBeingClicked.setBackgroundColor(Color.LTGRAY);

        final TableCol col = fieldToTableCol.get(cellBeingClicked);
        final GraphToListTranslator.SurveyListEntry surveyEntry = fieldToSurveyEntry.get(cellBeingClicked);

        switch (menuItem.getItemId()) {

            case R.id.setActiveStation:
                Station station = (Station)(GraphToListTranslator.createMap(surveyEntry).get(col));
                getSurvey().setActiveStation(station);
                return true;
            case R.id.deleteToEndOfBranch:
                new AlertDialog.Builder(this)
                    //.setIcon(android.R.drawable.ic_dialog_alert)
                    //.setTitle("Delete?") //R.string.quit)
                    .setMessage("Delete to end of branch?") //R.string.really_quit) FIXME
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Station from = surveyEntry.getFrom();
                            from.getOnwardLegs().remove(surveyEntry.getLeg());
                            // FIXME fix deleting branches with active station in

                            syncTableWithSurvey();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return true;
            default:
                return false;
        }

    }

}
