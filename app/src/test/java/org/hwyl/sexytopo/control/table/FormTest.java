package org.hwyl.sexytopo.control.table;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.widget.EditText;

import androidx.annotation.Nullable;

import junit.framework.TestCase;

import org.mockito.Mockito;

public class FormTest extends TestCase {
    static class MockForm extends Form {
        EditText name;
        EditText phone;
        boolean validName;
        boolean validPhone;

        MockForm() {
            super(Mockito.mock(Context.class));
            this.name = Mockito.mock(EditText.class);
            this.phone = Mockito.mock(EditText.class);

            this.validName = true;
            this.validPhone = true;
        }

        @Override
        protected void performValidation() {
            if (!validName) {
                this.setError(this.name, "invalid name");
            } else {
                this.setError(this.name, (CharSequence) null);
            }

            if (!validPhone) {
                this.setError(this.phone, "invalid phone");
            } else {
                this.setError(this.phone, (CharSequence) null);
            }
        }
    }

    static class MockValidateCallback implements Form.OnDidValidateCallback {
        @Nullable Boolean value = null;

        @Override
        public void onDidValidate(Boolean valid) {
            this.value = valid;
        }
    };

    private final MockForm form = new MockForm();

    public void testValidateWithValidFields() {
        form.validate();
        assert(form.isValid());
    }

    public void testValidateWithSingleInvalidField() {
        form.validName = false;

        form.validate();
        assert(!form.isValid());
    }

    public void testValidateWithMultipleInvalidFields() {
        form.validName = false;
        form.validPhone = false;

        form.validate();
        assert(!form.isValid());
    }

    public void testDelegatesSetErrorToFields() {
        form.name = Mockito.mock(EditText.class);
        form.validName = true;
        form.phone = Mockito.mock(EditText.class);
        form.validPhone = false;

        form.validate();

        verify(form.name).setError(null);
        verify(form.phone).setError("invalid phone");
    }

    public void testCallsOnDidValidateCallbackWithValidForm() {
        MockValidateCallback callback = new MockValidateCallback();
        form.setOnDidValidateCallback(callback);
        form.validate();

        assert(Boolean.TRUE.equals(callback.value));
    }

    public void testCallsOnDidValidateCallbackWithInvalidForm() {
        MockValidateCallback callback = new MockValidateCallback();
        form.setOnDidValidateCallback(callback);

        form.validPhone = false;
        form.validate();

        assert(Boolean.FALSE.equals(callback.value));
    }
}
