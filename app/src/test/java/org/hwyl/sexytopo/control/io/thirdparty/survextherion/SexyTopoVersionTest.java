package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import org.junit.Assert;
import org.junit.Test;

public class SexyTopoVersionTest {

    // --- extractFromText ---

    @Test
    public void testVersionExtractedFromTherionHeader() {
        String text = "# Created with SexyTopo 1.11.3 on 2025-03-01\nsurvey test\n";
        SexyTopoVersion version = SexyTopoVersion.extractFromText(text);
        Assert.assertNotNull(version);
        Assert.assertEquals("1.11.3", version.toString());
    }

    @Test
    public void testVersionExtractedFromSurvexHeader() {
        String text = "; Created with SexyTopo 1.11.2 on 2024-06-01\n*begin test\n";
        SexyTopoVersion version = SexyTopoVersion.extractFromText(text);
        Assert.assertNotNull(version);
        Assert.assertEquals("1.11.2", version.toString());
    }

    @Test
    public void testVersionNotFoundReturnsNullForThirdPartyFile() {
        String text = "; Exported by CaveSnail 3.0\n*begin test\n1 2 5.0 0.0 0.0\n";
        SexyTopoVersion version = SexyTopoVersion.extractFromText(text);
        Assert.assertNull(version);
    }

    @Test
    public void testVersionNotFoundReturnsNullForEmptyFile() {
        SexyTopoVersion version = SexyTopoVersion.extractFromText("");
        Assert.assertNull(version);
    }

    @Test
    public void testVersionExtractedFromMiddleOfFile() {
        // Header is not necessarily on the very first line
        String text = "survey test\n# Created with SexyTopo 2.0.1 on 2026-01-01\ncentreline\n";
        SexyTopoVersion version = SexyTopoVersion.extractFromText(text);
        Assert.assertNotNull(version);
        Assert.assertEquals("2.0.1", version.toString());
    }

    @Test
    public void testVersionExtractedWithoutCreatedWithPrefix() {
        // Surrounding wording is irrelevant — only the "SexyTopo X.Y.Z" token matters
        String text = "; Exported by SexyTopo 1.12.0\n*begin test\n";
        SexyTopoVersion version = SexyTopoVersion.extractFromText(text);
        Assert.assertNotNull(version);
        Assert.assertEquals("1.12.0", version.toString());
    }

    @Test
    public void testVersionTokenOnDataLineIsIgnored() {
        // A SexyTopo X.Y.Z token outside a comment line must not be treated as a version stamp
        String text = "*begin SexyTopo 9.9.9\n1 2 5.0 0.0 0.0\n";
        Assert.assertNull(SexyTopoVersion.extractFromText(text));
    }

    @Test
    public void testVersionTokenInTherionCommentIsExtracted() {
        String text = "survey test\n   # SexyTopo 2.0.1\ncentreline\n";
        SexyTopoVersion version = SexyTopoVersion.extractFromText(text);
        Assert.assertNotNull(version);
        Assert.assertEquals("2.0.1", version.toString());
    }

    @Test
    public void testIncompleteVersionTokenIsIgnored() {
        // "SexyTopo 1.11" has no patch component — not a match
        String text = "# Created with SexyTopo 1.11 on 2025-03-01\n";
        Assert.assertNull(SexyTopoVersion.extractFromText(text));
    }

    @Test
    public void testNullTextReturnsNull() {
        Assert.assertNull(SexyTopoVersion.extractFromText(null));
    }

    // --- isAfter ---

    @Test
    public void testVersionIsAfterOlderVersion() {
        SexyTopoVersion newer = new SexyTopoVersion(1, 11, 3);
        SexyTopoVersion older = new SexyTopoVersion(1, 11, 2);
        Assert.assertTrue(newer.isAfter(older));
    }

    @Test
    public void testVersionIsNotAfterSelf() {
        SexyTopoVersion v = new SexyTopoVersion(1, 11, 2);
        Assert.assertFalse(v.isAfter(v));
    }

    @Test
    public void testVersionIsNotAfterNewerVersion() {
        SexyTopoVersion older = new SexyTopoVersion(1, 11, 2);
        SexyTopoVersion newer = new SexyTopoVersion(1, 11, 3);
        Assert.assertFalse(older.isAfter(newer));
    }

    @Test
    public void testMajorVersionComparison() {
        SexyTopoVersion v2 = new SexyTopoVersion(2, 0, 0);
        SexyTopoVersion v1 = new SexyTopoVersion(1, 11, 2);
        Assert.assertTrue(v2.isAfter(v1));
        Assert.assertFalse(v1.isAfter(v2));
    }

    @Test
    public void testMinorVersionComparison() {
        SexyTopoVersion v = new SexyTopoVersion(1, 12, 0);
        SexyTopoVersion cutoff = new SexyTopoVersion(1, 11, 2);
        Assert.assertTrue(v.isAfter(cutoff));
    }

    // --- cutoff constant behaviour ---

    @Test
    public void testCutoffVersion1_11_2IsNotAfterItself() {
        Assert.assertFalse(
                SexyTopoVersion.LEG_COMMENTS_VERSION_CUTOFF.isAfter(
                        SexyTopoVersion.LEG_COMMENTS_VERSION_CUTOFF));
    }

    @Test
    public void testVersion1_11_3IsAfterCutoff() {
        SexyTopoVersion v = new SexyTopoVersion(1, 11, 3);
        Assert.assertTrue(v.isAfter(SexyTopoVersion.LEG_COMMENTS_VERSION_CUTOFF));
    }

    // --- useLegComments logic ---

    @Test
    public void testUseLegCommentsIsTrueWhenVersionIsNull() {
        // No SexyTopo header → treat as new format
        SexyTopoVersion version = null;
        boolean useLegComments =
                version == null || version.isAfter(SexyTopoVersion.LEG_COMMENTS_VERSION_CUTOFF);
        Assert.assertTrue(useLegComments);
    }

    @Test
    public void testUseLegCommentsIsTrueWhenVersionIsAfterCutoff() {
        SexyTopoVersion version = new SexyTopoVersion(1, 11, 3);
        boolean useLegComments =
                version == null || version.isAfter(SexyTopoVersion.LEG_COMMENTS_VERSION_CUTOFF);
        Assert.assertTrue(useLegComments);
    }

    @Test
    public void testUseLegCommentsIsFalseWhenVersionIsAtCutoff() {
        SexyTopoVersion version = new SexyTopoVersion(1, 11, 2);
        boolean useLegComments =
                version == null || version.isAfter(SexyTopoVersion.LEG_COMMENTS_VERSION_CUTOFF);
        Assert.assertFalse(useLegComments);
    }

    @Test
    public void testUseLegCommentsIsFalseWhenVersionIsBeforeCutoff() {
        SexyTopoVersion version = new SexyTopoVersion(1, 10, 9);
        boolean useLegComments =
                version == null || version.isAfter(SexyTopoVersion.LEG_COMMENTS_VERSION_CUTOFF);
        Assert.assertFalse(useLegComments);
    }
}
