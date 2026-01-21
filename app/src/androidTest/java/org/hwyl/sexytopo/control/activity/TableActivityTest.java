package org.hwyl.sexytopo.control.activity;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import org.hwyl.sexytopo.R;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class TableActivityTest {

    @Rule
    public ActivityScenarioRule<StartUpActivity> activityRule =
            new ActivityScenarioRule<>(StartUpActivity.class);


    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.WRITE_EXTERNAL_STORAGE");


    @Test
    public void tableHeaderVisible() {
        onView(withId(R.id.action_table)).perform(click());
        onView(withId(R.id.HeaderTable)).check(matches(isDisplayed()));
    }

    @Test
    public void tableRotationDoesNotCrash() {
        onView(withId(R.id.action_table)).perform(click());
        onView(withId(R.id.HeaderTable)).check(matches(isDisplayed()));

        activityRule.getScenario().onActivity(activity -> {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        });

        onView(withId(R.id.HeaderTable)).check(matches(isDisplayed()));
        onView(withId(R.id.tableRecyclerView)).check(matches(isDisplayed()));
        onView(withId(R.id.fabAddStation)).check(matches(isDisplayed()));
        onView(withId(R.id.fabAddSplay)).check(matches(isDisplayed()));

        activityRule.getScenario().onActivity(activity -> {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        });

        onView(withId(R.id.HeaderTable)).check(matches(isDisplayed()));
        onView(withId(R.id.tableRecyclerView)).check(matches(isDisplayed()));
        onView(withId(R.id.fabAddStation)).check(matches(isDisplayed()));
        onView(withId(R.id.fabAddSplay)).check(matches(isDisplayed()));
    }
}
