package org.hwyl.sexytopo.control.util;

import java.util.List;
import org.hwyl.sexytopo.model.survey.Licence;
import org.junit.Assert;
import org.junit.Test;

public class GeneralPreferencesLicenceTest {

    @Test
    public void testDefaultNamesAreOfferedInOrder() {
        List<String> names = Licence.getDefaultNames();

        Assert.assertEquals(7, names.size());
        Assert.assertEquals("GPLv3.0+", names.get(0));
        Assert.assertEquals("CC0", names.get(1));
        Assert.assertEquals("CC BY 4.0", names.get(2));
        Assert.assertEquals("CC BY-SA 4.0", names.get(3));
        Assert.assertEquals("CC BY-NC 4.0", names.get(4));
        Assert.assertEquals("CC BY-NC-SA 4.0", names.get(5));
        Assert.assertEquals("All rights reserved", names.get(6));
    }

    @Test
    public void testRecommendedLicenceIsGplv3() {
        Assert.assertEquals("GPLv3.0+", Licence.RECOMMENDED.getName());
        Assert.assertTrue(Licence.isDefault(Licence.RECOMMENDED.getName()));
    }

    @Test
    public void testIsDefaultRecognisesDefaultsOnly() {
        Assert.assertTrue(Licence.isDefault("CC0"));
        Assert.assertFalse(Licence.isDefault("CC BY-NC-ND 3.0 AT"));
        Assert.assertFalse(Licence.isDefault(Licence.NONE));
    }

    @Test
    public void testEveryDefaultLicenceHasASummary() {
        for (Licence licence : Licence.values()) {
            Assert.assertNotEquals(0, licence.getSummaryId());
        }
    }

    @Test
    public void testOnlyAllRightsReservedHasNoUrl() {
        for (Licence licence : Licence.values()) {
            Assert.assertEquals(licence != Licence.ALL_RIGHTS_RESERVED, licence.hasUrl());
        }
    }

    @Test
    public void testOnlyAllRightsReservedIsNotFree() {
        // Every licence that lets other cavers build on the survey counts as free here,
        // including the non-commercial ones.
        for (Licence licence : Licence.values()) {
            Assert.assertEquals(licence != Licence.ALL_RIGHTS_RESERVED, licence.isFree());
        }
    }

    @Test
    public void testSummaryPrefixFollowsFreedom() {
        for (Licence licence : Licence.values()) {
            Assert.assertEquals(
                    licence.isFree() ? Licence.FREE_PREFIX : Licence.WARNING_PREFIX,
                    licence.getSummaryPrefix());
        }
    }

    @Test
    public void testRecommendedLicenceIsFree() {
        Assert.assertTrue(Licence.RECOMMENDED.isFree());
    }

    @Test
    public void testForNameFindsDefaultsAndNothingElse() {
        Assert.assertEquals(Licence.CC0, Licence.forName("CC0"));
        Assert.assertNull(Licence.forName("CC BY-NC-ND 3.0 AT"));
        Assert.assertNull(Licence.forName(Licence.NONE));
    }

    @Test
    public void testNoLicenceIsTheEmptyString() {
        // Stored as "" so that a trip which is deliberately unlicensed behaves on export exactly
        // like one that never had a licence set.
        Assert.assertEquals("", Licence.NONE);
    }

    @Test
    public void testOfferedNamesAreDefaultsThenUsedThenNoLicence() {
        // Without a Context there are no stored used-licences, so the offered list is just the
        // defaults followed by the "no licence" entry.
        List<String> offered = GeneralPreferences.getLicenceNames();

        Assert.assertEquals(Licence.getDefaultNames().size() + 1, offered.size());
        Assert.assertEquals(Licence.getDefaultNames(), offered.subList(0, offered.size() - 1));
        Assert.assertEquals(Licence.NONE, offered.get(offered.size() - 1));
    }

    @Test
    public void testNoLicenceRememberedUntilOneIsChosen() {
        Assert.assertFalse(GeneralPreferences.hasLastLicence());
        Assert.assertEquals(Licence.NONE, GeneralPreferences.getLastLicence());
    }

    @Test
    public void testUsedLicencesAreEmptyWithoutPrefs() {
        Assert.assertTrue(GeneralPreferences.getUsedLicences().isEmpty());
    }
}
