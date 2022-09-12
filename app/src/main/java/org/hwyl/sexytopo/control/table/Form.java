package org.hwyl.sexytopo.control.table;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

abstract public class Form {
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

    private boolean valid;

    Form() {
        this.valid = true;
    }

    public Boolean isValid() {
        return this.valid;
    }

    public void validate() {
        this.valid = true;
        performValidation();
    }

    abstract protected void performValidation();

    protected void setError(TextView field, CharSequence error) {
        boolean fieldValid = (error == null);

        this.valid = this.valid  & fieldValid;
        field.setError(error);
    }
}
