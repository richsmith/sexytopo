package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.survey.LicenceOption;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;

public class GeneralPreferencesLicenceTest {

    @Test
    public void testGetLicenceOptionsReturnsSeedDefaultsWhenNoPrefs() {
        List<LicenceOption> options = GeneralPreferences.getLicenceOptions();

        Assert.assertEquals(6, options.size());
        Assert.assertEquals("GPLv3.0+", options.get(0).getName());
        Assert.assertEquals("CC0", options.get(1).getName());
        Assert.assertEquals("CC BY 4.0", options.get(2).getName());
        Assert.assertEquals("CC BY SA 4.0", options.get(3).getName());
        Assert.assertEquals("CC BY SA NC 4.0", options.get(4).getName());
        Assert.assertEquals("All rights reserved", options.get(5).getName());
        Assert.assertTrue(options.get(0).isDefault());

        int defaultCount = 0;
        for (LicenceOption option : options) {
            if (option.isDefault()) defaultCount++;
        }
        Assert.assertEquals(1, defaultCount);
    }

    @Test
    public void testGetDefaultLicenceNameReturnsGplv3WhenUnset() {
        Assert.assertEquals("GPLv3.0+", GeneralPreferences.getDefaultLicenceName());
    }

    @Test
    public void testLicenceOptionsToJsonAndFromJsonRoundTrip() throws Exception {
        List<LicenceOption> options =
                Arrays.asList(new LicenceOption("CC0", false), new LicenceOption("GPLv3.0+", true));

        JSONArray json = GeneralPreferences.licenceOptionsToJson(options);
        List<LicenceOption> loaded = GeneralPreferences.licenceOptionsFromJson(json.toString());

        Assert.assertEquals(options, loaded);
    }

    @Test
    public void testLicenceOptionsFromJsonDefaultsMissingDefaultFlagToFalse() throws Exception {
        String json = "[{\"name\":\"CC0\"}]";
        List<LicenceOption> loaded = GeneralPreferences.licenceOptionsFromJson(json);

        Assert.assertEquals(1, loaded.size());
        Assert.assertEquals("CC0", loaded.get(0).getName());
        Assert.assertFalse(loaded.get(0).isDefault());
    }

    @Test
    public void testWithAddedAppendsNonDefaultEntryToNonEmptyList() {
        List<LicenceOption> options =
                Collections.singletonList(new LicenceOption("GPLv3.0+", true));
        List<LicenceOption> result = GeneralPreferences.withAdded(options, "CC0");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("CC0", result.get(1).getName());
        Assert.assertFalse(result.get(1).isDefault());
        Assert.assertTrue(result.get(0).isDefault());
    }

    @Test
    public void testWithAddedToEmptyListBecomesDefault() {
        List<LicenceOption> result = GeneralPreferences.withAdded(new ArrayList<>(), "CC0");

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get(0).isDefault());
    }

    @Test
    public void testWithRemovedPromotesFirstRemainingWhenDefaultRemoved() {
        List<LicenceOption> options =
                Arrays.asList(
                        new LicenceOption("CC0", false),
                        new LicenceOption("GPLv3.0+", true),
                        new LicenceOption("CC BY 4.0", false));

        List<LicenceOption> result = GeneralPreferences.withRemoved(options, "GPLv3.0+");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("CC0", result.get(0).getName());
        Assert.assertTrue(result.get(0).isDefault());
        Assert.assertFalse(result.get(1).isDefault());
    }

    @Test
    public void testWithRemovedLeavesDefaultUnchangedWhenNonDefaultRemoved() {
        List<LicenceOption> options =
                Arrays.asList(new LicenceOption("CC0", false), new LicenceOption("GPLv3.0+", true));

        List<LicenceOption> result = GeneralPreferences.withRemoved(options, "CC0");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("GPLv3.0+", result.get(0).getName());
        Assert.assertTrue(result.get(0).isDefault());
    }

    @Test
    public void testWithRemovedLastOptionResultsInEmptyList() {
        List<LicenceOption> options =
                Collections.singletonList(new LicenceOption("GPLv3.0+", true));
        List<LicenceOption> result = GeneralPreferences.withRemoved(options, "GPLv3.0+");

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testWithRenamedPreservesPositionAndDefaultFlag() {
        List<LicenceOption> options =
                Arrays.asList(new LicenceOption("CC0", false), new LicenceOption("GPLv3.0+", true));

        List<LicenceOption> result =
                GeneralPreferences.withRenamed(options, "GPLv3.0+", "GPLv3.0 only");

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("CC0", result.get(0).getName());
        Assert.assertEquals("GPLv3.0 only", result.get(1).getName());
        Assert.assertTrue(result.get(1).isDefault());
    }

    @Test
    public void testWithDefaultSetUnsetsPreviousDefault() {
        List<LicenceOption> options =
                Arrays.asList(new LicenceOption("CC0", true), new LicenceOption("GPLv3.0+", false));

        List<LicenceOption> result = GeneralPreferences.withDefaultSet(options, "GPLv3.0+");

        Assert.assertFalse(result.get(0).isDefault());
        Assert.assertTrue(result.get(1).isDefault());
    }

    @Test
    public void testWithDefaultSetIsNoOpForUnknownName() {
        List<LicenceOption> options =
                Arrays.asList(new LicenceOption("CC0", true), new LicenceOption("GPLv3.0+", false));

        List<LicenceOption> result = GeneralPreferences.withDefaultSet(options, "Unknown Licence");

        Assert.assertEquals(options, result);
    }
}
