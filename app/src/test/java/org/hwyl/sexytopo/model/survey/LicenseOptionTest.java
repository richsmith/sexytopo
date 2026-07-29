package org.hwyl.sexytopo.model.survey;

import org.junit.Assert;
import org.junit.Test;

public class LicenseOptionTest {

    @Test
    public void testEqualsSameNameAndDefault() {
        LicenseOption a = new LicenseOption("GPLv3.0+", true);
        LicenseOption b = new LicenseOption("GPLv3.0+", true);
        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentName() {
        LicenseOption a = new LicenseOption("GPLv3.0+", true);
        LicenseOption b = new LicenseOption("CC0", true);
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentDefaultFlag() {
        LicenseOption a = new LicenseOption("GPLv3.0+", true);
        LicenseOption b = new LicenseOption("GPLv3.0+", false);
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testWithDefaultReturnsNewInstanceWithSameName() {
        LicenseOption original = new LicenseOption("CC0", false);
        LicenseOption updated = original.withDefault(true);

        Assert.assertEquals("CC0", updated.getName());
        Assert.assertTrue(updated.isDefault());
        Assert.assertFalse(original.isDefault());
    }

    @Test
    public void testWithNameReturnsNewInstanceWithSameDefaultFlag() {
        LicenseOption original = new LicenseOption("CC0", true);
        LicenseOption renamed = original.withName("CC0 (renamed)");

        Assert.assertEquals("CC0 (renamed)", renamed.getName());
        Assert.assertTrue(renamed.isDefault());
        Assert.assertEquals("CC0", original.getName());
    }

    @Test
    public void testToStringReturnsName() {
        LicenseOption option = new LicenseOption("CC BY 4.0", false);
        Assert.assertEquals("CC BY 4.0", option.toString());
    }
}
