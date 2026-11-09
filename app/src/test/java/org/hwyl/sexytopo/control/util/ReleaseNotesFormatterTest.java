package org.hwyl.sexytopo.control.util;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseNotesFormatterTest {

    @Test
    public void testHeadingsBecomeH2() {
        String html = ReleaseNotesFormatter.toHtml("# 2026-09-25 1.13.1\n");
        Assert.assertTrue(html.contains("<h2>2026-09-25 1.13.1</h2>"));
    }

    @Test
    public void testConsecutiveItemsShareOneList() {
        String html = ReleaseNotesFormatter.toHtml("# 1.0\n- One\n- Two\n\n# 0.9\n- Three\n");
        Assert.assertTrue(html.contains("<ul>\n<li>One</li>\n<li>Two</li>\n</ul>\n<h2>0.9</h2>"));
        Assert.assertTrue(html.contains("<ul>\n<li>Three</li>\n</ul>\n</body>"));
    }

    @Test
    public void testHtmlIsEscaped() {
        String html = ReleaseNotesFormatter.toHtml("- Text containing < or > & stuff\n");
        Assert.assertTrue(html.contains("<li>Text containing &lt; or &gt; &amp; stuff</li>"));
    }

    @Test
    public void testOtherLinesBecomeParagraphs() {
        String html = ReleaseNotesFormatter.toHtml("Some prose\n");
        Assert.assertTrue(html.contains("<p>Some prose</p>"));
    }
}
