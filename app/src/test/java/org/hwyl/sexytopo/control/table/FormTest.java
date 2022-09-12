package org.hwyl.sexytopo.control.table;

import static org.mockito.Mockito.verify;

import android.widget.EditText;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;

public class FormTest extends TestCase {
    class MockForm extends Form {
        EditText name;
        EditText phone;
        boolean validName;
        boolean validPhone;

        MockForm() {
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
                this.setError(this.name, null);
            }

            if (!validPhone) {
                this.setError(this.phone, "invalid phone");
            } else {
                this.setError(this.phone, null);
            }
        }
    }

    private MockForm form = new MockForm();

    @Test
    public void testValidateWithValidFields() {
        form.validate();
        assert(form.isValid());
    }

    @Test
    public void testValidateWithSingleInvalidField() {
        form.validName = false;

        form.validate();
        assert(!form.isValid());
    }

    @Test
    public void testValidateWithMultipleInvalidFields() {
        form.validName = false;
        form.validPhone = false;

        form.validate();
        assert(!form.isValid());
    }

    @Test
    public void testDelegatesSetErrorToFields() {
        form.name = Mockito.mock(EditText.class);
        form.validName = true;
        form.phone = Mockito.mock(EditText.class);
        form.validPhone = false;

        form.validate();

        verify(form.name).setError(null);
        verify(form.phone).setError("invalid phone");
    }
}
