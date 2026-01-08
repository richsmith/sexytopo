package org.hwyl.sexytopo.control.activity;

import org.hwyl.sexytopo.R;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class DistoXCalibrationActivityTest {

    @Rule
    public ActivityScenarioRule<DistoXCalibrationActivity> activityRule =
            new ActivityScenarioRule<>(DistoXCalibrationActivity.class);


    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.WRITE_EXTERNAL_STORAGE");


    @Test
    public void calibrationStartButtonVisible() {
        onView(withId(R.id.calibration_start)).check(matches(isDisplayed()));
    }
}
