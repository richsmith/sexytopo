package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.view.View;
import android.widget.AutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import org.hwyl.sexytopo.R;

public class TeamMemberForm extends Form {

    private final TextInputLayout nameLayout;
    private final AutoCompleteTextView nameField;

    public TeamMemberForm(Context context, View dialogView) {
        super(context);
        this.nameLayout = dialogView.findViewById(R.id.name_input_layout);
        this.nameField = dialogView.findViewById(R.id.name_field);
        this.nameField.addTextChangedListener(new TextViewValidationTrigger(this));
    }

    public String getName() {
        return nameField.getText().toString().trim();
    }

    public void setName(String name) {
        nameField.setText(name);
    }

    @Override
    protected void performValidation() {
        if (nameField.getText().toString().trim().isEmpty()) {
            setError(nameLayout, R.string.trip_dialog_name_required);
        } else {
            setError(nameLayout, (Integer) null);
        }
    }
}
