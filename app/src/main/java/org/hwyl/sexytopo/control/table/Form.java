package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;

public abstract class Form {
    private final Context context;

    static class TextViewValidationTrigger implements TextWatcher {
        final Form form;

        TextViewValidationTrigger(Form form) {
            this.form = form;
        }

        @Override
        public void beforeTextChanged(
                CharSequence charSequence, int start, int before, int count) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable editable) {
            this.form.validate();
        }
    }

    public interface OnDidValidateCallback {
        void onDidValidate(Boolean valid);
    }

    protected boolean valid;
    private boolean showErrors;
    private boolean showRangeErrors;
    @Nullable private OnDidValidateCallback onDidValidateCallback;

    Form(Context context) {
        this.context = context;
        this.valid = true;
        this.showErrors = false;
        this.showRangeErrors = false;
        this.onDidValidateCallback = null;
    }

    public void enableErrors() {
        this.showErrors = true;
    }

    public void enableRangeErrors() {
        this.showRangeErrors = true;
    }

    public void setOnDidValidateCallback(@Nullable OnDidValidateCallback callback) {
        this.onDidValidateCallback = callback;
    }

    public Boolean isValid() {
        return this.valid;
    }

    public void validate() {
        this.valid = true;
        performValidation();

        if (this.onDidValidateCallback != null) {
            this.onDidValidateCallback.onDidValidate(this.valid);
        }
    }

    protected abstract void performValidation();

    protected void setError(TextView field, CharSequence error) {
        boolean fieldValid = (error == null);

        this.valid = this.valid & fieldValid;
        field.setError(showErrors ? error : null);
    }

    protected void setError(TextView field, Integer error) {
        CharSequence message = error == null ? null : context.getString(error);
        this.setError(field, message);
    }

    protected void setError(TextInputLayout layout, CharSequence error) {
        this.valid = this.valid & (error == null);
        layout.setError(showErrors ? error : null);
    }

    protected void setError(TextInputLayout layout, Integer error) {
        CharSequence message = error == null ? null : context.getString(error);
        this.setError(layout, message);
    }

    protected void setRangeError(TextInputLayout layout, CharSequence error) {
        this.valid = this.valid & (error == null);
        layout.setError(showRangeErrors ? error : null);
    }

    protected void setRangeError(TextInputLayout layout, Integer error) {
        CharSequence message = error == null ? null : context.getString(error);
        this.setRangeError(layout, message);
    }
}
