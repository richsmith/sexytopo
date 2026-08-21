package org.hwyl.sexytopo.control.activity;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.is;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.rule.GrantPermissionRule;
import org.hwyl.sexytopo.R;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class TripActivityTest {

    @Rule
    public ActivityScenarioRule<StartUpActivity> activityRule =
            new ActivityScenarioRule<>(StartUpActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.WRITE_EXTERNAL_STORAGE");

    @Test
    public void tripActivityLaunches() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_view)).perform(click());
        onView(withText(R.string.action_trip)).perform(click());
        onView(withId(R.id.rootLayout)).check(matches(isDisplayed()));
    }

    @Test
    public void licenceFieldShowsFullSuggestionListEvenWhenPrefilled() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_view)).perform(click());
        onView(withText(R.string.action_trip)).perform(click());

        // The Licence field is pre-filled with the default licence (GPLv3.0+) when
        // screen opens. Tapping it should still offer every configured licence, not just the
        // one matching entry - confirming the field doesn't fall back to stock ArrayAdapter
        // filtering behaviour.
        onView(withId(R.id.trip_licence)).perform(click());

        onData(is("All rights reserved")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
        onData(is("CC0")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
        onData(is("GPLv3.0+")).inRoot(isPlatformPopup()).check(matches(isDisplayed()));
    }
}
