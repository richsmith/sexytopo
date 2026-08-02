package org.hwyl.sexytopo.control.activity;

import android.widget.Button;
import android.widget.Spinner;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class LicenseChoiceActivityTest {

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
    }

    private static LicenseChoiceActivity buildActivity() {
        ActivityController<LicenseChoiceActivity> controller =
                Robolectric.buildActivity(LicenseChoiceActivity.class).setup();
        return controller.get();
    }

    @Test
    public void testSpinnerIsPopulatedFromLicenseOptions() {
        LicenseChoiceActivity activity = buildActivity();
        Spinner spinner = activity.findViewById(R.id.license_choice_spinner);

        Assert.assertEquals(GeneralPreferences.getLicenseOptions().size(), spinner.getCount());
    }

    @Test
    public void testSpinnerIsPreselectedToGplv3() {
        LicenseChoiceActivity activity = buildActivity();
        Spinner spinner = activity.findViewById(R.id.license_choice_spinner);

        Assert.assertEquals("GPLv3.0+", spinner.getSelectedItem());
    }

    @Test
    public void testConfirmPersistsSelectedChoiceAndSetsConfirmedFlag() {
        LicenseChoiceActivity activity = buildActivity();
        Spinner spinner = activity.findViewById(R.id.license_choice_spinner);

        int ccZeroIndex = -1;
        for (int i = 0; i < spinner.getCount(); i++) {
            if ("CC0".equals(spinner.getItemAtPosition(i))) {
                ccZeroIndex = i;
                break;
            }
        }
        Assert.assertTrue("CC0 should be one of the seed license options", ccZeroIndex >= 0);
        spinner.setSelection(ccZeroIndex);

        Button confirmButton = activity.findViewById(R.id.license_choice_confirm_button);
        confirmButton.performClick();

        Assert.assertTrue(GeneralPreferences.isLicenseDefaultConfirmed());
        Assert.assertEquals("CC0", GeneralPreferences.getDefaultLicenseName());
        Assert.assertTrue(activity.isFinishing());
    }

    @Test
    public void testBackPressAlsoConfirmsWhateverIsCurrentlySelected() {
        LicenseChoiceActivity activity = buildActivity();

        activity.onBackPressed();

        Assert.assertTrue(GeneralPreferences.isLicenseDefaultConfirmed());
        Assert.assertEquals("GPLv3.0+", GeneralPreferences.getDefaultLicenseName());
        Assert.assertTrue(activity.isFinishing());
    }
}
