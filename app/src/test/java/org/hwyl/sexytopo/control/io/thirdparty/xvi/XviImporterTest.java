package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import junit.framework.Assert;

import org.junit.Test;


public class XviImporterTest {

    @Test
    public void testGetEmptyBlockContents() throws Exception {
        String simpleText = "set Command{}";
        String contents = XviImporter.getBlockContents(simpleText, "set Command");
        Assert.assertEquals("", contents);
    }

    @Test
    public void testGetEmptyBlockContentsWithWhitespace() throws Exception {
        String simpleText = "set Command {}";
        String contents = XviImporter.getBlockContents(simpleText, "set Command");
        Assert.assertEquals("", contents);
    }

    @Test
    public void testGetSimpleBlockContents() throws Exception {
        String simpleText = "set Command {foo}";
        String contents = XviImporter.getBlockContents(simpleText, "set Command");
        Assert.assertEquals("foo", contents);
    }

    @Test
    public void testGetNestedBlockContents() throws Exception {
        String simpleText = "set Command {{foo}}";
        String contents = XviImporter.getBlockContents(simpleText, "set Command");
        Assert.assertEquals("{foo}", contents);
    }
}
