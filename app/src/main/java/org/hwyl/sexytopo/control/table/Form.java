package org.hwyl.sexytopo.control.table;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.annotation.Nullable;

abstract public class Form {
    private final Context context;

    static class TextViewValidationTrigger implements TextWatcher {
        private final Form form;

        TextViewValidationTrigger(Form form) {
            this.form = form;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {}

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

    private boolean valid;
    @Nullable private OnDidValidateCallback onDidValidateCallback;

    Form(Context context) {
        this.context = context;
        this.valid = true;
        this.onDidValidateCallback = null;
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

        if(this.onDidValidateCallback != null) {
            this.onDidValidateCallback.onDidValidate(this.valid);
        }
    }

    abstract protected void performValidation();

    protected void setError(TextView field, CharSequence error) {
        boolean fieldValid = (error == null);

        this.valid = this.valid  & fieldValid;
        field.setError(error);
    }

    protected void setError(TextView field, Integer error) {
        CharSequence message = error == null? null : context.getString(error);
        this.setError(field, message);
    }
}
