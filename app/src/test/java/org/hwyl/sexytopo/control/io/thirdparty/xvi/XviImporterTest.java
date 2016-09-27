package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import junit.framework.Assert;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.junit.Test;

import java.util.List;


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

    @Test(expected=Exception.class)
    public void testBadlyNestedBlockContentsRaiseError() throws Exception {
        String simpleText = "set Command {{foo}";
        String contents = XviImporter.getBlockContents(simpleText, "set Command");
        Assert.assertEquals("{foo}", contents);
    }

    @Test(expected=Exception.class)
    public void testNoCommandMatchRaiseError() throws Exception {
        String simpleText = "set Commandx {foo}";
        String contents = XviImporter.getBlockContents(simpleText, "set Command");
    }


    @Test
    public void testParseBlockEntriesSingle() throws Exception {
        String simpleText = "{foo}";
        List<String> contents = XviImporter.parseBlockEntries(simpleText);
        Assert.assertEquals("foo", contents.get(0));
    }

    @Test
    public void testParseBlockEntriesMultiple() throws Exception {
        String simpleText = "{foo}{bar}";
        List<String> contents = XviImporter.parseBlockEntries(simpleText);
        Assert.assertEquals("foo", contents.get(0));
        Assert.assertEquals("bar", contents.get(1));
    }

    @Test
    public void testParseBlockEntriesWithMultipleItems() throws Exception {
        String simpleText = "{foo extra}\n{bar more}";
        List<String> contents = XviImporter.parseBlockEntries(simpleText);
        Assert.assertEquals("foo extra", contents.get(0));
        Assert.assertEquals("bar more", contents.get(1));
    }

    @Test
    public void testParseBlockEntriesMultipleWithWhitespace() throws Exception {
        String simpleText = "\t{foo}\n   {bar}";
        List<String> contents = XviImporter.parseBlockEntries(simpleText);
        Assert.assertEquals("foo", contents.get(0));
        Assert.assertEquals("bar", contents.get(1));
    }

    @Test
    public void testParseSketchEntryGetsRightColour() throws Exception {
        String simpleText = "red 0 0";
        PathDetail pathDetail = XviImporter.parseSketchEntry(simpleText);
        Assert.assertEquals(Colour.RED, pathDetail.getColour());
    }

    @Test
    public void testParseSketchEntryParsesFirstEntry() throws Exception {
        String simpleText = "red 0 0";
        PathDetail pathDetail = XviImporter.parseSketchEntry(simpleText);
        Assert.assertEquals(Coord2D.ORIGIN, pathDetail.getPath().get(0));
        Assert.assertEquals(1, pathDetail.getPath().size());
    }

    @Test
    public void testParseSketchEntryParsesSecondEntry() throws Exception {
        String simpleText = "blue 0 0 1 0";
        PathDetail pathDetail = XviImporter.parseSketchEntry(simpleText);
        Assert.assertEquals(new Coord2D(1, 0), pathDetail.getPath().get(1));
        Assert.assertEquals(2, pathDetail.getPath().size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseSketchEntryFailsForUnevenData() throws Exception {
        String simpleText = "red 0 0 1";
        PathDetail pathDetail = XviImporter.parseSketchEntry(simpleText);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseSketchEntryFailsForUnknownFirstCommand() throws Exception {
        String simpleText = "connect 0 0";
        PathDetail pathDetail = XviImporter.parseSketchEntry(simpleText);
    }
}
