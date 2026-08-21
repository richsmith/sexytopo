package org.hwyl.sexytopo.model.survey;

import org.junit.Assert;
import org.junit.Test;

public class LicenceOptionTest {

    @Test
    public void testEqualsSameNameAndDefault() {
        LicenceOption a = new LicenceOption("GPLv3.0+", true);
        LicenceOption b = new LicenceOption("GPLv3.0+", true);
        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentName() {
        LicenceOption a = new LicenceOption("GPLv3.0+", true);
        LicenceOption b = new LicenceOption("CC0", true);
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentDefaultFlag() {
        LicenceOption a = new LicenceOption("GPLv3.0+", true);
        LicenceOption b = new LicenceOption("GPLv3.0+", false);
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testWithDefaultReturnsNewInstanceWithSameName() {
        LicenceOption original = new LicenceOption("CC0", false);
        LicenceOption updated = original.withDefault(true);

        Assert.assertEquals("CC0", updated.getName());
        Assert.assertTrue(updated.isDefault());
        Assert.assertFalse(original.isDefault());
    }

    @Test
    public void testWithNameReturnsNewInstanceWithSameDefaultFlag() {
        LicenceOption original = new LicenceOption("CC0", true);
        LicenceOption renamed = original.withName("CC0 (renamed)");

        Assert.assertEquals("CC0 (renamed)", renamed.getName());
        Assert.assertTrue(renamed.isDefault());
        Assert.assertEquals("CC0", original.getName());
    }

    @Test
    public void testToStringReturnsName() {
        LicenceOption option = new LicenceOption("CC BY 4.0", false);
        Assert.assertEquals("CC BY 4.0", option.toString());
    }
}
