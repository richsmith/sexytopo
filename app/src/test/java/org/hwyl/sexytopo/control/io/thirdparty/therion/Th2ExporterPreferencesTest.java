package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.SharedPreferences;
import java.util.List;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** How the preferences affect the cross-sections in the Therion export. */
@RunWith(RobolectricTestRunner.class)
public class Th2ExporterPreferencesTest {

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
        prefs = GeneralPreferences.getRawPreferences();
        Assert.assertNotNull(prefs);
    }

    @Test
    public void testElevationCrossSectionNamesFollowTheElevationSuffixPreference() {
        prefs.edit().putString("pref_therion_ee_xs_suffix", "SEC-#").apply();
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        List<String> elevation =
                Th2ExporterTest.getSectionPointNames(
                        Th2ExporterTest.export(survey, Projection2D.EXTENDED_ELEVATION));
        List<String> plan =
                Th2ExporterTest.getSectionPointNames(
                        Th2ExporterTest.export(survey, Projection2D.PLAN));

        Assert.assertTrue(Th2ExporterTest.anyEndWith(elevation, "SEC-1"));
        Assert.assertTrue(Th2ExporterTest.anyEndWith(elevation, "SEC-3"));
        // The plan has its own preference, which hasn't been changed
        Assert.assertTrue(Th2ExporterTest.anyEndWith(plan, "PX1"));
    }

    @Test
    public void testPlanCrossSectionNamesFollowThePlanSuffixPreference() {
        prefs.edit().putString("pref_therion_plan_xs_suffix", "S#").apply();
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        List<String> plan =
                Th2ExporterTest.getSectionPointNames(
                        Th2ExporterTest.export(survey, Projection2D.PLAN));
        List<String> elevation =
                Th2ExporterTest.getSectionPointNames(
                        Th2ExporterTest.export(survey, Projection2D.EXTENDED_ELEVATION));

        Assert.assertTrue(Th2ExporterTest.anyEndWith(plan, "S1"));
        Assert.assertTrue(Th2ExporterTest.anyEndWith(elevation, "EEX1"));
    }

    @Test
    public void testNoCrossSectionsAreExportedWhenTheOptionIsOff() {
        prefs.edit().putBoolean("pref_therion_cross_sections", false).apply();
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        for (Projection2D projection :
                new Projection2D[] {Projection2D.PLAN, Projection2D.EXTENDED_ELEVATION}) {
            String th2 = Th2ExporterTest.export(survey, projection);

            Assert.assertTrue(
                    projection.name(), Th2ExporterTest.getSectionPointNames(th2).isEmpty());
            Assert.assertTrue(
                    projection.name(), Th2ExporterTest.getSectionScrapNames(th2).isEmpty());
        }
    }
}
