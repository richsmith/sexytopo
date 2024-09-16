package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Trip;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class TripActivity extends SexyTopoActivity implements View.OnClickListener {

    @SuppressLint("SimpleDateFormat")
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


    private List<Trip.TeamEntry> team = new ArrayList<>();
    ArrayAdapter<Trip.TeamEntry> teamListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        ListView personList = findViewById(R.id.person_list);
        teamListAdapter = new TeamListArrayAdapter(this, team);
        personList.setAdapter(teamListAdapter);


        TextView commentsView = findViewById(R.id.trip_comments);
        commentsView.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                updateButtonStatus();
                syncTrip();
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        Trip trip = getSurvey().getTrip();
        if (trip == null) {
            trip = new Trip();
        }

        team = new ArrayList<>(trip.getTeam());

        String comments = trip.getComments();
        TextView commentsField = findViewById(R.id.trip_comments);
        commentsField.setText(comments);

        Date date = trip.getDate();
        String formatted = DATE_FORMAT.format(date);
        TextView dateField = findViewById(R.id.trip_date);
        dateField.setText(
            getText(R.string.trip) + " " + formatted + ". " + getText(R.string.trip_team) + ":");

        syncListWithTeam();
        updateButtonStatus();
    }


    public void requestClear(View view) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.trip_dialog_confirm_clear_trip)
            .setPositiveButton(R.string.clear, (dialog, whichButton) -> {
                EditText comments = findViewById(R.id.trip_comments);
                comments.setText("");
                team.clear();
                syncListWithTeam();
                updateButtonStatus();
            }).setNegativeButton(R.string.cancel, null)
            .show();
    }


    public void requestAddEntry(View view) {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText nameField = new EditText(this);
        nameField.setHint(R.string.trip_dialog_add_to_team_name_hint);
        nameField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(nameField);

        // Create a custom adapter
        final ArrayAdapter<Trip.Role> arrayAdapter = new ArrayAdapter<Trip.Role>(
            this, android.R.layout.simple_list_item_multiple_choice, Trip.Role.values()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Trip.Role role = getItem(position);
                ((TextView) view).setText(role != null ? getString(role.descriptionId) : "");
                return view;
            }
        };

        final ListView roleList = new ListView(this);
        roleList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        roleList.setAdapter(arrayAdapter);
        layout.addView(roleList);

        AlertDialog.Builder builderSingle =
            new AlertDialog.Builder(this)
                .setTitle(R.string.trip_dialog_title_add_to_team)
                .setView(layout)
                .setPositiveButton(R.string.add,
                    (dialog, which) -> {
                        String name = nameField.getText().toString();
                        SparseBooleanArray checked = roleList.getCheckedItemPositions();
                        List<Trip.Role> selectedRoles = new ArrayList<>();
                        for (int i = 0; i < Trip.Role.values().length; i++) {
                            if (checked.get(i)) {
                                selectedRoles.add(Trip.Role.values()[i]);
                            }
                        }
                        addTeamMember(name, selectedRoles);
                        dialog.dismiss();
                    })
                .setNegativeButton(R.string.cancel,
                    (dialog, which) -> dialog.dismiss());

        builderSingle.show();
    }

    public void addTeamMember(String name, List<Trip.Role> roles) {
        if (roles.isEmpty()) {
            roles.add(Trip.Role.EXPLORATION);
        }
        Trip.TeamEntry newTeamEntry = new Trip.TeamEntry(name, roles);
        team.add(newTeamEntry);
        syncTrip();
        syncListWithTeam();
    }


    public void setTeamMember(int position, String name, List<Trip.Role> roles) {
        if (roles.isEmpty()) {
            roles.add(Trip.Role.EXPLORATION);
        }
        Trip.TeamEntry newTeamEntry = new Trip.TeamEntry(name, roles);
        team.set(position, newTeamEntry);
        syncTrip();
        syncListWithTeam();
    }

    protected void saveSurvey() {
        syncTrip();
        super.saveSurvey();
    }

    public void requestSaveTrip(View view) {
        syncTrip();
        startActivity(PlanActivity.class);
    }


    public void syncListWithTeam() {
        teamListAdapter = new TeamListArrayAdapter(this, team);
        ListView personList = findViewById(R.id.person_list);
        personList.setAdapter(teamListAdapter);
        updateButtonStatus();
    }

    private void syncTrip() {
        EditText commentsField = findViewById(R.id.trip_comments);
        String comments = commentsField.getText().toString();

        Trip trip = getSurvey().getTrip();
        if (trip == null) {
            trip = new Trip();
        }
        trip.setTeam(team);
        trip.setComments(comments);
        getSurvey().setTrip(trip);
    }



    @SuppressWarnings("UnnecessaryLocalVariable")
    public void requestDeleteSelected(View view) {
        TeamListArrayAdapter adapter = getTeamListArrayAdapter();
        List<Trip.TeamEntry> toKeep = adapter.getUnchecked();
        team = toKeep;
        syncTrip();
        syncListWithTeam();
    }


    public void updateButtonStatus() {
        TeamListArrayAdapter adapter = getTeamListArrayAdapter();
        List<Trip.TeamEntry> unchecked = adapter.getUnchecked();
        Button deleteButton = findViewById(R.id.delete_person);
        deleteButton.setEnabled(unchecked.size() < team.size());

        Button startButton = findViewById(R.id.set_trip);
        boolean hasTeam = !team.isEmpty();
        startButton.setEnabled(hasTeam);

        Button clearButton = findViewById(R.id.clear_trip);
        TextView commentsView = findViewById(R.id.trip_comments);
        boolean hasComments = !commentsView.getText().toString().isEmpty();
        clearButton.setEnabled(hasTeam || hasComments);
    }

    private TeamListArrayAdapter getTeamListArrayAdapter() {
        ListView listView = findViewById(R.id.person_list);
        return (TeamListArrayAdapter)listView.getAdapter();
    }


    @Override
    public void onClick(View view) {
        final int position = (int) view.getTag();
        Trip.TeamEntry teamEntry = team.get(position);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText nameField = new EditText(this);
        nameField.setHint(R.string.trip_dialog_add_to_team_name_hint);
        nameField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameField.setText(teamEntry.name);
        layout.addView(nameField);

        final ArrayAdapter<Trip.Role> arrayAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_list_item_multiple_choice);
        arrayAdapter.addAll(Trip.Role.values());
        final ListView roleList = new ListView(this);

        roleList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        roleList.setAdapter(arrayAdapter);

        Trip.Role[] allRoles = Trip.Role.values();
        for (int i = 0; i < allRoles.length; i++) {
            Trip.Role role = allRoles[i];
            if (teamEntry.roles.contains(role)) {
                roleList.setItemChecked(i, true);
            }
        }

        layout.addView(roleList);

        AlertDialog.Builder builderSingle =
            new AlertDialog.Builder(this)
                .setTitle(R.string.edit)
                .setView(layout)
                .setPositiveButton(R.string.save,
                    (dialog, which) -> {
                        String name = nameField.getText().toString();
                        SparseBooleanArray checked = roleList.getCheckedItemPositions();
                        List<Trip.Role> selectedRoles = new ArrayList<>();
                        for (int i = 0; i < Trip.Role.values().length; i++) {
                            if (checked.get(i)) {
                                selectedRoles.add(Trip.Role.values()[i]);
                            }
                        }
                        setTeamMember(position, name, selectedRoles);
                        dialog.dismiss();
                    })
                .setNegativeButton(R.string.cancel,
                        (dialog, which) -> dialog.dismiss());

        builderSingle.show();
    }


    public class TeamListArrayAdapter extends ArrayAdapter<Trip.TeamEntry> {

        private final Context context;
        private final boolean[] checked;
        private final List<Trip.TeamEntry> team;

        public TeamListArrayAdapter(Context context, List<Trip.TeamEntry> team) {
            super(
                context,
                android.R.layout.simple_list_item_multiple_choice,
                team.toArray(new Trip.TeamEntry[]{}));
            this.context = context;
            this.team = team;
            this.checked = new boolean[team.size()];
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Trip.TeamEntry teamEntry = getItem(position);
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.trip_team_member_item, parent, false);
            rowView.setTag(position);
            TextView nameField = rowView.findViewById(R.id.name_field);
            nameField.setText(teamEntry.name);
            TextView roleField = rowView.findViewById(R.id.role_field);

            List<String> roleDescriptions = new ArrayList<>();
            for (Trip.Role role : teamEntry.roles) {
                roleDescriptions.add(context.getString(role.descriptionId));
            }
            String roles = TextTools.join(", ", roleDescriptions);
            roleField.setText(roles);

            final CheckBox checkBox = rowView.findViewById(R.id.settings_notification_checkbox);
            checkBox.setOnClickListener(view -> {
                checked[position] = checkBox.isChecked();
                updateButtonStatus();
            });
            rowView.setOnClickListener(TripActivity.this);
            return rowView;
        }

        public List<Trip.TeamEntry> getUnchecked() {
            List<Trip.TeamEntry> unchecked = new ArrayList<>();
            for (int i = 0; i < team.size(); i++) {
                if (!checked[i]) {
                    unchecked.add(team.get(i));
                }
            }
            return unchecked;
        }

    }
}
