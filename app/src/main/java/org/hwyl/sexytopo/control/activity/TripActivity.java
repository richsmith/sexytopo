package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.Instrument;
import org.hwyl.sexytopo.control.table.TeamMemberForm;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Licence;
import org.hwyl.sexytopo.model.survey.Trip;

public class TripActivity extends SexyTopoActivity {

    @SuppressLint("SimpleDateFormat")
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final int[] ROLE_CHECKBOX_IDS = {
        R.id.role_book, R.id.role_instruments, R.id.role_dog, R.id.role_exploration
    };

    private List<Trip.TeamEntry> team = new ArrayList<>();
    private Trip savedTrip;

    // Suppresses syncTrip() calls triggered by TextWatchers firing as a side effect of the
    // setText(...) calls in onResume()'s field population - without this, populating an
    // earlier field (e.g. comments) fires its watcher, which calls syncTrip(), which reads
    // every field's *current* text including ones not yet populated this cycle, clobbering
    // them with stale values before they ever get set.
    private boolean isPopulatingFields = false;

    // Whether the licence question has been answered for the trip currently on screen - either
    // because it was loaded with a licence already set, or because the user has picked one (or
    // explicitly picked "no licence") this session. Until it has, the Save button stays disabled,
    // nudging the user into making a choice rather than leaving it blank by default. A blank
    // licence is a perfectly valid answer; it just has to be chosen rather than defaulted into.
    private boolean isLicenceChosen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        setupMaterialToolbar();
        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        TextView commentsView = findViewById(R.id.trip_comments);
        commentsView.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {}

                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (isPopulatingFields) {
                            return;
                        }
                        updateButtonStatus();
                        syncTrip();
                    }
                });

        EditText instrumentField = findViewById(R.id.instrument_field);
        instrumentField.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (isPopulatingFields) {
                            return;
                        }
                        syncTrip();
                        updateButtonStatus();
                    }

                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                });

        EditText copyrightHolderField = findViewById(R.id.trip_copyright_holder);
        copyrightHolderField.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (isPopulatingFields) {
                            return;
                        }
                        syncTrip();
                        updateButtonStatus();
                    }

                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                });

        AutoCompleteTextView licenceField = findViewById(R.id.trip_licence);
        licenceField.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (isPopulatingFields) {
                            return;
                        }
                        // Typing a licence by hand answers the question just as picking one from
                        // the dropdown does. Clearing the field by hand does not: an empty field
                        // is the un-answered state, and "no licence" is chosen from the dropdown.
                        if (!s.toString().trim().isEmpty()) {
                            isLicenceChosen = true;
                        }
                        syncTrip();
                        updateButtonStatus();
                    }

                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Trip trip = getSurvey().getTrip();
        boolean isNewTrip = trip == null;
        if (isNewTrip) {
            trip = new Trip();
            getSurvey().setTrip(trip);
        }

        // What the trip looked like before anything was seeded onto it. Taking this snapshot
        // first is what makes seeded values register as unsaved changes, so the Save button is
        // enabled on arrival and the user can commit them - rather than the screen showing a
        // licence and copyright holder that were never actually written to the survey.
        savedTrip = new Trip(trip);

        if (isNewTrip) {
            // Seed a brand new trip with the licence the user chose last time, so it carries
            // over between trips. Until they have chosen one this is blank, so no licence is
            // ever applied to a survey without having been picked. This only ever runs once,
            // for a genuinely new trip - it must not re-apply on every visit to an existing
            // trip, since a blank licence there could mean "the user deliberately wants no
            // licence" (or the trip was imported with none), not "never decided".
            trip.setLicence(GeneralPreferences.getLastLicence());

            // Likewise the copyright holder: a caver generally surveys under the same name, or
            // their club's, each time.
            trip.setCopyrightHolder(GeneralPreferences.getLastCopyrightHolder());
        }
        team = new ArrayList<>(trip.getTeam());

        isPopulatingFields = true;
        try {
            TextView commentsField = findViewById(R.id.trip_comments);
            commentsField.setText(trip.getComments());

            EditText instrumentField = findViewById(R.id.instrument_field);
            if (trip.hasInstrument()) {
                instrumentField.setText(trip.getInstrument());
            } else if (hasInstrument()) {
                try {
                    Instrument connected = getInstrument();
                    String name = connected.getName();
                    instrumentField.setText(name);
                } catch (SecurityException ignored) {
                }
            }

            setupCopyrightHolderAutocomplete();
            AutoCompleteTextView copyrightHolderField = findViewById(R.id.trip_copyright_holder);
            // false: don't filter on this text, which would pop the suggestion list open over
            // the screen as it loads.
            copyrightHolderField.setText(trip.getCopyrightHolder(), false);

            setupLicenceAutocomplete();
            AutoCompleteTextView licenceField = findViewById(R.id.trip_licence);
            licenceField.setText(trip.getLicence());

            // The licence question counts as answered if this trip already carries a licence, or
            // if the user has previously chosen one - including when that choice was "no
            // licence", which is remembered as such and leaves the field legitimately blank.
            // Either way, don't make them re-pick just to save an unrelated edit.
            isLicenceChosen = trip.hasLicence() || GeneralPreferences.hasLastLicence();
        } finally {
            isPopulatingFields = false;
        }

        TextInputLayout surveyDateLayout = findViewById(R.id.survey_date_layout);
        TextInputEditText surveyDateField = findViewById(R.id.survey_date_field);
        surveyDateField.setOnClickListener(this::onChooseSurveyDateClicked);
        surveyDateLayout.setEndIconOnClickListener(this::onChooseSurveyDateClicked);

        TextInputLayout exploDateLayout = findViewById(R.id.exploration_date_layout);
        TextInputEditText exploDateField = findViewById(R.id.exploration_date_field);
        exploDateField.setOnClickListener(this::onChooseExplorationDateClicked);
        exploDateLayout.setEndIconOnClickListener(this::onChooseExplorationDateClicked);

        CheckBox linkedCheckbox = findViewById(R.id.exploration_date_linked_checkbox);
        linkedCheckbox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    Trip t = getSurvey().getTrip();
                    if (t == null) return;
                    t.setExplorationDateLinked(isChecked);
                    if (isChecked) {
                        t.setExplorationDate(null);
                    }
                    syncTrip();
                    updateDateDisplay();
                    updateButtonStatus();
                });

        Button clearExploButton = findViewById(R.id.clear_exploration_date_button);
        clearExploButton.setOnClickListener(
                v -> {
                    Trip t = getSurvey().getTrip();
                    if (t == null) return;
                    t.setExplorationDate(null);
                    t.setExplorationDateLinked(false);
                    syncTrip();
                    updateDateDisplay();
                    updateButtonStatus();
                });

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
        } catch (SecurityException ignored) {
        }
    }

    public void onChooseSurveyDateClicked(View view) {
        Trip trip = getSurvey().getTrip();
        MaterialDatePicker<Long> picker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText(R.string.trip_survey_date_label)
                        .setSelection(trip.getSurveyDate().getTime())
                        .build();

        picker.addOnPositiveButtonClickListener(
                selection -> {
                    trip.setSurveyDate(new Date(selection));
                    updateDateDisplay();
                    syncTrip();
                    updateButtonStatus();
                });

        FragmentManager fm = getSupportFragmentManager();
        picker.show(fm, "survey_date_picker");
    }

    public void onChooseExplorationDateClicked(View view) {
        Trip trip = getSurvey().getTrip();
        if (trip == null || trip.isExplorationDateLinked()) return;

        Date seed =
                trip.getExplorationDate() != null
                        ? trip.getExplorationDate()
                        : trip.getSurveyDate();
        MaterialDatePicker<Long> picker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText(R.string.trip_exploration_date_label)
                        .setSelection(seed.getTime())
                        .build();

        picker.addOnPositiveButtonClickListener(
                selection -> {
                    trip.setExplorationDate(new Date(selection));
                    updateDateDisplay();
                    syncTrip();
                    updateButtonStatus();
                });

        FragmentManager fm = getSupportFragmentManager();
        picker.show(fm, "explo_date_picker");
    }

    public void requestClear(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.trip_dialog_confirm_clear_trip)
                .setPositiveButton(
                        R.string.clear,
                        (dialog, whichButton) -> {
                            EditText comments = findViewById(R.id.trip_comments);
                            comments.setText("");
                            AutoCompleteTextView copyrightHolder =
                                    findViewById(R.id.trip_copyright_holder);
                            copyrightHolder.setText("", false);
                            AutoCompleteTextView licence = findViewById(R.id.trip_licence);
                            licence.setText("");
                            isLicenceChosen = false;
                            team.clear();
                            Trip trip = getSurvey().getTrip();
                            if (trip != null) {
                                trip.setExplorationDate(null);
                                trip.setExplorationDateLinked(true);
                            }
                            syncListWithTeam();
                            updateDateDisplay();
                            updateButtonStatus();
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Offers the known cavers as suggestions for the copyright holder, since it's often one of the
     * surveyors. Only a suggestion: the field stays free text, as the holder is just as likely to
     * be a club or an expedition. Suggestions appear once the user starts typing rather than on
     * focus, because free text is the common case here.
     */
    private void setupCopyrightHolderAutocomplete() {
        AutoCompleteTextView holderField = findViewById(R.id.trip_copyright_holder);
        List<String> knownCavers = GeneralPreferences.getKnownCavers();
        holderField.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, knownCavers));
    }

    private void setupLicenceAutocomplete() {
        AutoCompleteTextView licenceField = findViewById(R.id.trip_licence);

        // Two parallel lists: what each row shows, and what picking it puts in the field. They
        // differ where a row is decorated - the "no licence" option is stored under an empty
        // name that would otherwise show as a blank row, and the recommended licence is
        // labelled as such - so the decoration never leaks into the saved licence.
        List<String> storedNames = GeneralPreferences.getLicenceNames();
        List<String> displayLabels = new ArrayList<>();
        for (String name : storedNames) {
            displayLabels.add(licenceDisplayLabel(name));
        }

        // The stock ArrayAdapter filters its suggestions against the text that is already in the
        // field, which would narrow the dropdown down to only the entries matching whatever has
        // been typed or carried over from the last trip. A non-filtering adapter keeps the full
        // configured list showing regardless of the current text, while the field itself stays
        // editable, so free text works.
        ArrayAdapter<String> adapter =
                new NonFilteringArrayAdapter(
                        this, android.R.layout.simple_dropdown_item_1line, displayLabels);
        licenceField.setAdapter(adapter);
        licenceField.setThreshold(0);

        // Picking from the dropdown counts as answering the licence question, including when the
        // answer is "no licence" - which leaves the field empty, since that is how it's stored.
        licenceField.setOnItemClickListener(
                (parent, view, position, id) -> {
                    isLicenceChosen = true;
                    licenceField.setText(storedNames.get(position));
                    syncTrip();
                    updateButtonStatus();
                });

        View.OnClickListener showFullDropdown =
                v -> {
                    if (!displayLabels.isEmpty()) {
                        licenceField.showDropDown();
                    }
                };
        // Tapping the field re-opens the list even if it's already focused (e.g. after the
        // dropdown was previously dismissed), and gaining focus opens it the first time.
        licenceField.setOnClickListener(showFullDropdown);
        licenceField.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus && !displayLabels.isEmpty()) {
                        licenceField.showDropDown();
                    }
                });
    }

    /**
     * How a licence option is shown in a list, as opposed to how it is stored: the "no licence"
     * option gets a readable label in place of its empty name, and the recommended licence is
     * tagged as such. Every other name is shown as-is.
     */
    private String licenceDisplayLabel(String storedName) {
        if (storedName.isEmpty()) {
            return getString(R.string.trip_licence_none);
        }
        if (storedName.equals(Licence.RECOMMENDED.getName())) {
            return getString(R.string.trip_licence_recommended_suffix, storedName);
        }
        return storedName;
    }

    /**
     * An ArrayAdapter whose suggestion list is not narrowed by the current text of the field it's
     * attached to. Used for the Licence field so the full configured list is always offered, even
     * though the field is pre-filled with a default value.
     */
    private static class NonFilteringArrayAdapter extends ArrayAdapter<String> {

        private final List<String> items;

        private final Filter noOpFilter =
                new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = items;
                        results.count = items.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        // Deliberately do nothing: the backing data set (and therefore the
                        // dropdown contents) never changes in response to typed text.
                    }
                };

        NonFilteringArrayAdapter(Context context, int resource, List<String> items) {
            super(context, resource, items);
            this.items = items;
        }

        @Override
        public @NonNull Filter getFilter() {
            return noOpFilter;
        }
    }

    private void setupNameAutocomplete(View dialogView) {
        AutoCompleteTextView nameField = dialogView.findViewById(R.id.name_field);
        List<String> knownCavers = GeneralPreferences.getKnownCavers();
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, knownCavers);
        nameField.setAdapter(adapter);
        nameField.setThreshold(0);
        nameField.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus && !knownCavers.isEmpty()) {
                        nameField.showDropDown();
                    }
                });
    }

    public void requestAddEntry(View view) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trip_team_member, null);
        TeamMemberForm form = new TeamMemberForm(this, dialogView);
        setupNameAutocomplete(dialogView);

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.trip_dialog_title_add_to_team)
                        .setView(dialogView)
                        .setPositiveButton(R.string.add, null)
                        .setNegativeButton(R.string.cancel, null)
                        .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
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

        // Remember this trip's licence as the choice to carry over to the next new trip, and,
        // if it isn't one of the defaults, add it to the list offered in future - the same way
        // a caver's name is remembered once it's been used.
        AutoCompleteTextView licenceField = findViewById(R.id.trip_licence);
        GeneralPreferences.setLastLicence(licenceField.getText().toString());

        AutoCompleteTextView copyrightHolderField = findViewById(R.id.trip_copyright_holder);
        GeneralPreferences.setLastCopyrightHolder(copyrightHolderField.getText().toString());

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
            row.findViewById(R.id.delete_button)
                    .setOnClickListener(v -> deleteTeamMember(position));

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

        EditText copyrightHolderField = findViewById(R.id.trip_copyright_holder);
        trip.setCopyrightHolder(copyrightHolderField.getText().toString());

        AutoCompleteTextView licenceField = findViewById(R.id.trip_licence);
        trip.setLicence(licenceField.getText().toString());

        getSurvey().setTrip(trip);
    }

    public void updateButtonStatus() {
        boolean hasTeam = !team.isEmpty();

        TextView commentsView = findViewById(R.id.trip_comments);
        boolean hasComments = !commentsView.getText().toString().trim().isEmpty();

        EditText instrumentField = findViewById(R.id.instrument_field);
        boolean hasInstrument = !instrumentField.getText().toString().trim().isEmpty();

        Trip currentTrip = getSurvey().getTrip();
        boolean hasUnlinkedExploDate =
                currentTrip != null && !currentTrip.isExplorationDateLinked();

        AutoCompleteTextView copyrightHolderField = findViewById(R.id.trip_copyright_holder);
        boolean hasCopyrightHolder = !copyrightHolderField.getText().toString().trim().isEmpty();

        AutoCompleteTextView licenceField = findViewById(R.id.trip_licence);
        boolean hasLicence = !licenceField.getText().toString().trim().isEmpty();

        // Values seeded from the last trip count as data like anything else: they're shown on
        // screen, so they have to be saveable, or the survey would end up without the licence
        // and copyright holder the user was looking at. hasChanges is what keeps the buttons
        // from staying lit once everything on screen has actually been written.
        boolean hasAnyData =
                hasTeam
                        || hasComments
                        || hasInstrument
                        || hasCopyrightHolder
                        || hasLicence
                        || hasUnlinkedExploDate;
        boolean hasChanges = savedTrip == null || !savedTrip.equals(currentTrip);

        findViewById(R.id.set_trip).setEnabled(hasAnyData && hasChanges && isLicenceChosen);

        // Say why Save is disabled, but only when the licence is the one thing blocking it -
        // otherwise the button is disabled for a reason the user can already see (nothing
        // entered yet, or nothing changed since the last save) and the prompt would be noise.
        TextInputLayout licenceLayout = findViewById(R.id.trip_licence_layout);
        licenceLayout.setHelperText(
                hasAnyData && hasChanges && !isLicenceChosen
                        ? getString(R.string.trip_licence_required_helper)
                        : null);
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
                ((CheckBox) dialogView.findViewById(ROLE_CHECKBOX_IDS[i])).setChecked(true);
            }
        }

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.edit)
                        .setView(dialogView)
                        .setPositiveButton(R.string.save, null)
                        .setNegativeButton(R.string.cancel, null)
                        .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            form.validate();
                            if (!form.isValid()) return;
                            setTeamMember(position, form.getName(), getCheckedRoles(dialogView));
                            dialog.dismiss();
                        });
    }

    private void updateDateDisplay() {
        Trip trip = getSurvey().getTrip();
        if (trip == null) return;

        TextInputEditText surveyDateField = findViewById(R.id.survey_date_field);
        surveyDateField.setText(DATE_FORMAT.format(trip.getSurveyDate()));

        TextInputLayout exploDateLayout = findViewById(R.id.exploration_date_layout);
        TextInputEditText exploDateField = findViewById(R.id.exploration_date_field);
        Button clearExploButton = findViewById(R.id.clear_exploration_date_button);
        CheckBox linkedCheckbox = findViewById(R.id.exploration_date_linked_checkbox);

        // Update checkbox without triggering the listener
        linkedCheckbox.setOnCheckedChangeListener(null);
        linkedCheckbox.setChecked(trip.isExplorationDateLinked());
        linkedCheckbox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    Trip t = getSurvey().getTrip();
                    if (t == null) return;
                    t.setExplorationDateLinked(isChecked);
                    if (isChecked) {
                        t.setExplorationDate(null);
                    }
                    syncTrip();
                    updateDateDisplay();
                    updateButtonStatus();
                });

        if (trip.isExplorationDateLinked()) {
            exploDateField.setText(DATE_FORMAT.format(trip.getSurveyDate()));
            exploDateField.setEnabled(false);
            exploDateLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            clearExploButton.setVisibility(View.GONE);
        } else {
            exploDateField.setEnabled(true);
            exploDateLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            exploDateLayout.setEndIconDrawable(R.drawable.ic_calendar);
            exploDateLayout.setEndIconOnClickListener(this::onChooseExplorationDateClicked);
            exploDateField.setOnClickListener(this::onChooseExplorationDateClicked);
            if (trip.getExplorationDate() != null) {
                exploDateField.setText(DATE_FORMAT.format(trip.getExplorationDate()));
            } else {
                exploDateField.setText("");
            }
            clearExploButton.setVisibility(View.VISIBLE);
        }
    }

    private List<Trip.Role> getCheckedRoles(View dialogView) {
        Trip.Role[] allRoles = Trip.Role.values();
        List<Trip.Role> selectedRoles = new ArrayList<>();
        for (int i = 0; i < allRoles.length; i++) {
            if (((CheckBox) dialogView.findViewById(ROLE_CHECKBOX_IDS[i])).isChecked()) {
                selectedRoles.add(allRoles[i]);
            }
        }
        return selectedRoles;
    }
}
