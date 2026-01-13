package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableRowAdapter extends RecyclerView.Adapter<TableRowAdapter.TableRowViewHolder> {

    private final List<GraphToListTranslator.SurveyListEntry> entries = new ArrayList<>();
    private final Map<TextView, GraphToListTranslator.SurveyListEntry> fieldToSurveyEntry = new HashMap<>();
    private final Map<TextView, TableCol> fieldToTableCol = new HashMap<>();
    private final Map<View, Integer> viewToPosition = new HashMap<>();
    private final List<Integer> columnWidths = new ArrayList<>();
    
    private final Context context;
    private Survey survey;
    private final OnRowClickListener onRowClickListener;

    private static final EnumMap<TableCol, Integer> TABLE_COL_TO_ANDROID_ID =
        new EnumMap<TableCol, Integer>(TableCol.class) {{
            put(TableCol.FROM, R.id.tableRowFrom);
            put(TableCol.TO, R.id.tableRowTo);
            put(TableCol.DISTANCE, R.id.tableRowDistance);
            put(TableCol.AZIMUTH, R.id.tableRowAzimuth);
            put(TableCol.INCLINATION, R.id.tableRowInclination);
        }};

    public interface OnRowClickListener {
        void onRowClick(View view, GraphToListTranslator.SurveyListEntry entry, TableCol col);
        void onRowLongClick(View view, GraphToListTranslator.SurveyListEntry entry, TableCol col);
    }

    public TableRowAdapter(Context context, Survey survey, OnRowClickListener listener) {
        this.context = context;
        this.survey = survey;
        this.onRowClickListener = listener;
    }

    public void setSurvey(Survey updatedSurvey) {
        this.survey = updatedSurvey;
    }

    public void setEntries(List<GraphToListTranslator.SurveyListEntry> newEntries) {
        fieldToSurveyEntry.clear();
        fieldToTableCol.clear();
        viewToPosition.clear();
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    public Map<TextView, GraphToListTranslator.SurveyListEntry> getFieldToSurveyEntry() {
        return fieldToSurveyEntry;
    }

    public Map<TextView, TableCol> getFieldToTableCol() {
        return fieldToTableCol;
    }

    @NonNull
    @Override
    public TableRowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.table_row, parent, false);
        return new TableRowViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TableRowViewHolder holder, int position) {
        GraphToListTranslator.SurveyListEntry entry = entries.get(position);
        Map<TableCol, Object> map = GraphToListTranslator.createMap(entry);

        for (TableCol col : TableCol.values()) {
            if (col == TableCol.COMMENT) {
                continue;
            }

            String display = map.containsKey(col) ? col.format(map.get(col)) : "?";
            int id = TABLE_COL_TO_ANDROID_ID.get(col);
            TextView textView = holder.itemView.findViewById(id);
            textView.setText(display);

            // Apply column width if available
            if (!columnWidths.isEmpty()) {
                TableCol[] cols = TableCol.values();
                int colIndex = 0;
                for (int i = 0; i < cols.length; i++) {
                    if (cols[i] != TableCol.COMMENT) {
                        if (cols[i] == col) {
                            if (colIndex < columnWidths.size()) {
                                android.view.ViewGroup.LayoutParams params = textView.getLayoutParams();
                                params.width = columnWidths.get(colIndex);
                                textView.setLayoutParams(params);
                            }
                            break;
                        }
                        colIndex++;
                    }
                }
            }

            // Set background color with alternate row shading (theme-aware)
            if (isActiveStation(map.get(col))) {
                // Highlight active station with theme-aware amber/yellow
                int bgColor = ContextCompat.getColor(context, R.color.tableHighlight);
                textView.setBackgroundColor(bgColor);
                // Set contrasting text color for readability
                int textColor = ContextCompat.getColor(context, R.color.tableHighlightText);
                textView.setTextColor(textColor);
            } else {
                // Reset text color to default for non-highlighted rows
                int textColor = ContextCompat.getColor(context, R.color.bodyTextColor);
                textView.setTextColor(textColor);

                if (position % 2 == 0) {
                    // Even rows - main background color
                    int bgColor = ContextCompat.getColor(context, R.color.tableBackground);
                    textView.setBackgroundColor(bgColor);
                } else {
                    // Odd rows - alternate background color
                    int bgColor = ContextCompat.getColor(context, R.color.tableBackgroundAlt);
                    textView.setBackgroundColor(bgColor);
                }
            }

            // Bold for full legs, normal for splays
            if (entry.getLeg().hasDestination()) {
                textView.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                textView.setTypeface(Typeface.DEFAULT);
            }

            // Set alignment based on column type
            if (col == TableCol.DISTANCE || col == TableCol.AZIMUTH || col == TableCol.INCLINATION) {
                // Right-align numeric columns
                textView.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            } else if (col == TableCol.FROM || col == TableCol.TO) {
                // Center-align station names
                textView.setGravity(android.view.Gravity.CENTER);
            } else {
                textView.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            }

            // Store mappings and position
            fieldToSurveyEntry.put(textView, entry);
            fieldToTableCol.put(textView, col);
            viewToPosition.put(textView, position);

            // Set click listener
            textView.setOnClickListener(v -> {
                if (onRowClickListener != null) {
                    onRowClickListener.onRowClick(v, entry, col);
                }
            });

            // Set long click listener
            textView.setOnLongClickListener(v -> {
                if (onRowClickListener != null) {
                    onRowClickListener.onRowLongClick(v, entry, col);
                }
                return true;
            });
        }
    }

    public void setColumnWidths(List<Integer> widths) {
        columnWidths.clear();
        columnWidths.addAll(widths);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public int getPositionForStation(Station station) {
        if (station == null) {
            return -1;
        }
        
        for (int i = 0; i < entries.size(); i++) {
            GraphToListTranslator.SurveyListEntry entry = entries.get(i);
            
            // Check if this is the destination station of a leg ("To" column)
            // We want to jump to the row where this station is the destination
            if (entry.getLeg().hasDestination()) {
                Station destination = entry.getLeg().getDestination();
                if (destination != null && destination.getName().equals(station.getName())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isActiveStation(Object object) {
        return (object instanceof Station) && object == survey.getActiveStation();
    }

    public static class TableRowViewHolder extends RecyclerView.ViewHolder {
        public TableRowViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
