package org.hwyl.sexytopo.model.survey;

import org.junit.Assert;
import org.junit.Test;

public class LegFilterTest {

    private static final Leg LEG = new Leg(5, 0, 0, new Station("2"), new Leg[] {});

    private static Leg enabledSplay() {
        return new Leg(1, 90, 0);
    }

    private static Leg disabledSplay() {
        Leg splay = new Leg(1, 270, 0);
        splay.setEnabled(false);
        return splay;
    }

    @Test
    public void testAllKeepsLegsAndEnabledSplays() {
        Assert.assertTrue(LegFilter.ALL.test(LEG));
        Assert.assertTrue(LegFilter.ALL.test(enabledSplay()));
        Assert.assertFalse(LegFilter.ALL.test(disabledSplay()));
    }

    @Test
    public void testLegsKeepsOnlyLegs() {
        Assert.assertTrue(LegFilter.LEGS.test(LEG));
        Assert.assertFalse(LegFilter.LEGS.test(enabledSplay()));
        Assert.assertFalse(LegFilter.LEGS.test(disabledSplay()));
    }

    @Test
    public void testSplaysKeepsOnlyEnabledSplays() {
        Assert.assertFalse(LegFilter.SPLAYS.test(LEG));
        Assert.assertTrue(LegFilter.SPLAYS.test(enabledSplay()));
        Assert.assertFalse(LegFilter.SPLAYS.test(disabledSplay()));
    }

    @Test
    public void testIncludeDisabledAddsDisabledSplaysOnly() {
        Assert.assertTrue(LegFilter.ALL.includeDisabled(true).test(disabledSplay()));
        Assert.assertTrue(LegFilter.SPLAYS.includeDisabled(true).test(disabledSplay()));
        Assert.assertFalse(LegFilter.SPLAYS.includeDisabled(true).test(LEG));
        Assert.assertFalse(LegFilter.LEGS.includeDisabled(true).test(disabledSplay()));
    }

    @Test
    public void testIncludeDisabledFalseLeavesThemOut() {
        LegFilter filter = LegFilter.SPLAYS.includeDisabled(true).includeDisabled(false);
        Assert.assertFalse(filter.test(disabledSplay()));
        Assert.assertTrue(filter.test(enabledSplay()));
    }

    @Test
    public void testIncludeDisabledLeavesTheOriginalFilterAlone() {
        LegFilter.SPLAYS.includeDisabled(true);
        Assert.assertFalse(LegFilter.SPLAYS.test(disabledSplay()));
    }

    @Test
    public void testIncludeFeaturesFalseLeavesOutSplaysToOtherFeatures() {
        LegFilter filter = LegFilter.SPLAYS.includeFeatures(false);
        Assert.assertTrue(filter.test(enabledSplay()));
        Assert.assertFalse(filter.test(enabledSplay().withToWall(false)));
        Assert.assertTrue(LegFilter.SPLAYS.test(enabledSplay().withToWall(false)));
    }

    @Test
    public void testIncludeFeaturesLeavesLegsAlone() {
        Assert.assertTrue(LegFilter.ALL.includeFeatures(false).test(LEG));
    }

    @Test
    public void testIncludeFeaturesAndIncludeDisabledCombine() {
        Leg disabledToFeature = disabledSplay().withToWall(false);
        Leg disabledToWall = disabledSplay();

        LegFilter filter = LegFilter.SPLAYS.includeDisabled(true).includeFeatures(false);

        Assert.assertTrue(filter.test(disabledToWall));
        Assert.assertFalse(filter.test(disabledToFeature));
    }
}
