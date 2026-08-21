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
public class LicenceChoiceActivityTest {

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
    }

    private static LicenceChoiceActivity buildActivity() {
        ActivityController<LicenceChoiceActivity> controller =
                Robolectric.buildActivity(LicenceChoiceActivity.class).setup();
        return controller.get();
    }

    @Test
    public void testSpinnerIsPopulatedFromLicenceOptions() {
        LicenceChoiceActivity activity = buildActivity();
        Spinner spinner = activity.findViewById(R.id.licence_choice_spinner);

        Assert.assertEquals(GeneralPreferences.getLicenceOptions().size(), spinner.getCount());
    }

    @Test
    public void testSpinnerIsPreselectedToGplv3() {
        LicenceChoiceActivity activity = buildActivity();
        Spinner spinner = activity.findViewById(R.id.licence_choice_spinner);

        Assert.assertEquals("GPLv3.0+", spinner.getSelectedItem());
    }

    @Test
    public void testConfirmPersistsSelectedChoiceAndSetsConfirmedFlag() {
        LicenceChoiceActivity activity = buildActivity();
        Spinner spinner = activity.findViewById(R.id.licence_choice_spinner);

        int ccZeroIndex = -1;
        for (int i = 0; i < spinner.getCount(); i++) {
            if ("CC0".equals(spinner.getItemAtPosition(i))) {
                ccZeroIndex = i;
                break;
            }
        }
        Assert.assertTrue("CC0 should be one of the seed licence options", ccZeroIndex >= 0);
        spinner.setSelection(ccZeroIndex);

        Button confirmButton = activity.findViewById(R.id.licence_choice_confirm_button);
        confirmButton.performClick();

        Assert.assertTrue(GeneralPreferences.isLicenceDefaultConfirmed());
        Assert.assertEquals("CC0", GeneralPreferences.getDefaultLicenceName());
        Assert.assertTrue(activity.isFinishing());
    }

    @Test
    public void testBackPressAlsoConfirmsWhateverIsCurrentlySelected() {
        LicenceChoiceActivity activity = buildActivity();

        activity.onBackPressed();

        Assert.assertTrue(GeneralPreferences.isLicenceDefaultConfirmed());
        Assert.assertEquals("GPLv3.0+", GeneralPreferences.getDefaultLicenceName());
        Assert.assertTrue(activity.isFinishing());
    }
}
