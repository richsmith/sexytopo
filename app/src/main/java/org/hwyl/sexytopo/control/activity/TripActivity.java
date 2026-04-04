package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.hwyl.sexytopo.comms.Instrument;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.table.TeamMemberForm;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Trip;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class TripActivity extends SexyTopoActivity {

    @SuppressLint("SimpleDateFormat")
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final int[] ROLE_CHECKBOX_IDS = {
        R.id.role_book, R.id.role_instruments, R.id.role_dog, R.id.role_exploration
    };

    private List<Trip.TeamEntry> team = new ArrayList<>();
    private Trip savedTrip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        setupMaterialToolbar();
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        TextView commentsView = findViewById(R.id.trip_comments);
        commentsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonStatus();
                syncTrip();
            }
        });

        EditText instrumentField = findViewById(R.id.instrument_field);
        instrumentField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                syncTrip();
                updateButtonStatus();
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        Trip trip = getSurvey().getTrip();
        if (trip == null) {
            trip = new Trip();
            getSurvey().setTrip(trip);
        }

        savedTrip = new Trip(trip);
        team = new ArrayList<>(trip.getTeam());

        TextView commentsField = findViewById(R.id.trip_comments);
        commentsField.setText(trip.getComments());

EditText instrumentField = findViewById(R.id.instrument_field);
        instrumentField.setText(trip.hasInstrument() ? trip.getInstrument() : "");

        TextInputLayout exploDateLayout = findViewById(R.id.exploration_date_layout);
        TextInputEditText exploDateField = findViewById(R.id.exploration_date_field);
        exploDateField.setOnClickListener(this::onChooseDateClicked);
        exploDateLayout.setEndIconOnClickListener(this::onChooseDateClicked);

        syncListWithTeam();
        updateButtonStatus();
        updateDateDisplay();
    }

    public void onGetInstrumentClicked(View view) {
        Instrument instrument = getInstrument();
        if (instrument == null) return;
        try {
            String name = instrument.getName();
            if (name != null) {
                EditText instrumentField = findViewById(R.id.instrument_field);
                instrumentField.setText(name);
            }
        } catch (SecurityException ignored) {}
    }

    public void onChooseDateClicked(View view) {
        Trip trip = getSurvey().getTrip();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.trip_exploration_date_label)
            .setSelection(trip.getDate().getTime())
            .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            trip.setDate(new Date(selection));
            updateDateDisplay();
            syncTrip();
            updateButtonStatus();
        });

        FragmentManager fm = getSupportFragmentManager();
        picker.show(fm, "date_picker");
    }

    public void requestClear(View view) {
        new MaterialAlertDialogBuilder(this)
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

    private void setupNameAutocomplete(View dialogView) {
        AutoCompleteTextView nameField = dialogView.findViewById(R.id.name_field);
        List<String> knownCavers = GeneralPreferences.getKnownCavers();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, knownCavers);
        nameField.setAdapter(adapter);
        nameField.setThreshold(0);
        nameField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !knownCavers.isEmpty()) {
                nameField.showDropDown();
            }
        });
    }

    public void requestAddEntry(View view) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trip_team_member, null);
        TeamMemberForm form = new TeamMemberForm(this, dialogView);
        setupNameAutocomplete(dialogView);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.trip_dialog_title_add_to_team)
            .setView(dialogView)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            form.validate();
            if (!form.isValid()) return;
            addTeamMember(form.getName(), getCheckedRoles(dialogView));
            dialog.dismiss();
        });
    }

    public void addTeamMember(String name, List<Trip.Role> roles) {
        GeneralPreferences.addKnownCaver(name);
        team.add(new Trip.TeamEntry(name, roles));
        syncTrip();
        syncListWithTeam();
    }

    public void setTeamMember(int position, String name, List<Trip.Role> roles) {
        GeneralPreferences.addKnownCaver(name);
        team.set(position, new Trip.TeamEntry(name, roles));
        syncTrip();
        syncListWithTeam();
    }

    public void deleteTeamMember(int position) {
        team.remove(position);
        syncTrip();
        syncListWithTeam();
    }

    protected void saveSurvey() {
        syncTrip();
        savedTrip = new Trip(getSurvey().getTrip());
        super.saveSurvey();
        updateButtonStatus();
    }

    public void requestSaveTrip(View view) {
        syncTrip();
        startActivity(PlanActivity.class);
    }

    public void syncListWithTeam() {
        LinearLayout container = findViewById(R.id.person_list);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < team.size(); i++) {
            final int position = i;
            Trip.TeamEntry entry = team.get(i);
            View row = inflater.inflate(R.layout.trip_team_member_item, container, false);

            ((TextView) row.findViewById(R.id.name_field)).setText(entry.name);

            List<String> roleDescriptions = new ArrayList<>();
            for (Trip.Role role : entry.roles) {
                roleDescriptions.add(getString(role.descriptionId));
            }
            ((TextView) row.findViewById(R.id.role_field))
                .setText(TextTools.join(", ", roleDescriptions));

            row.setOnClickListener(v -> editTeamMember(position));
            row.findViewById(R.id.delete_button).setOnClickListener(v -> deleteTeamMember(position));

            container.addView(row);
        }
        updateButtonStatus();
    }

    private void syncTrip() {
        EditText commentsField = findViewById(R.id.trip_comments);
        Trip trip = getSurvey().getTrip();
        if (trip == null) {
            trip = new Trip();
        }
        trip.setTeam(team);
        trip.setComments(commentsField.getText().toString());

        EditText instrumentField = findViewById(R.id.instrument_field);
        trip.setInstrument(instrumentField.getText().toString());

        getSurvey().setTrip(trip);
    }

    public void updateButtonStatus() {
        boolean hasTeam = !team.isEmpty();

        TextView commentsView = findViewById(R.id.trip_comments);
        boolean hasComments = !commentsView.getText().toString().trim().isEmpty();

        EditText instrumentField = findViewById(R.id.instrument_field);
        boolean hasInstrument = !instrumentField.getText().toString().trim().isEmpty();

        boolean hasAnyData = hasTeam || hasComments || hasInstrument;
        Trip currentTrip = getSurvey().getTrip();
        boolean hasChanges = savedTrip == null || !savedTrip.equals(currentTrip);

        findViewById(R.id.set_trip).setEnabled(hasTeam && hasChanges);
        findViewById(R.id.clear_trip).setEnabled(hasAnyData);

        Button getInstrumentButton = findViewById(R.id.instrument_get_button);
        getInstrumentButton.setEnabled(hasInstrument());
    }

    private void editTeamMember(int position) {
        Trip.TeamEntry teamEntry = team.get(position);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trip_team_member, null);
        TeamMemberForm form = new TeamMemberForm(this, dialogView);
        setupNameAutocomplete(dialogView);
        form.setName(teamEntry.name);

        Trip.Role[] allRoles = Trip.Role.values();
        for (int i = 0; i < allRoles.length; i++) {
            if (teamEntry.roles.contains(allRoles[i])) {
                ((MaterialCheckBox) dialogView.findViewById(ROLE_CHECKBOX_IDS[i])).setChecked(true);
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            form.validate();
            if (!form.isValid()) return;
            setTeamMember(position, form.getName(), getCheckedRoles(dialogView));
            dialog.dismiss();
        });
    }

    private void updateDateDisplay() {
        TextInputEditText dateField = findViewById(R.id.exploration_date_field);
        Trip trip = getSurvey().getTrip();
        if (trip == null) return;
        dateField.setText(DATE_FORMAT.format(trip.getDate()));
    }

    private List<Trip.Role> getCheckedRoles(View dialogView) {
        Trip.Role[] allRoles = Trip.Role.values();
        List<Trip.Role> selectedRoles = new ArrayList<>();
        for (int i = 0; i < allRoles.length; i++) {
            if (((MaterialCheckBox) dialogView.findViewById(ROLE_CHECKBOX_IDS[i])).isChecked()) {
                selectedRoles.add(allRoles[i]);
            }
        }
        return selectedRoles;
    }
}
