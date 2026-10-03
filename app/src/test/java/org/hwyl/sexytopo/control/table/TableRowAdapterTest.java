package org.hwyl.sexytopo.control.table;

import android.graphics.Paint;
import org.junit.Assert;
import org.junit.Test;

public class TableRowAdapterTest {

    private static final int OTHER_FLAGS = Paint.ANTI_ALIAS_FLAG | Paint.UNDERLINE_TEXT_FLAG;

    @Test
    public void testStrikeThroughIsAddedForHiddenRow() {
        int flags = TableRowAdapter.strikeThroughFlags(Paint.ANTI_ALIAS_FLAG, true);

        Assert.assertEquals(Paint.STRIKE_THRU_TEXT_FLAG, flags & Paint.STRIKE_THRU_TEXT_FLAG);
    }

    @Test
    public void testStrikeThroughIsRemovedForVisibleRow() {
        // Rows are recycled, so a row that was hidden must be reset when reused for another leg
        int recycled = Paint.ANTI_ALIAS_FLAG | Paint.STRIKE_THRU_TEXT_FLAG;

        int flags = TableRowAdapter.strikeThroughFlags(recycled, false);

        Assert.assertEquals(0, flags & Paint.STRIKE_THRU_TEXT_FLAG);
    }

    @Test
    public void testOtherPaintFlagsAreKept() {
        Assert.assertEquals(
                OTHER_FLAGS | Paint.STRIKE_THRU_TEXT_FLAG,
                TableRowAdapter.strikeThroughFlags(OTHER_FLAGS, true));
        Assert.assertEquals(
                OTHER_FLAGS,
                TableRowAdapter.strikeThroughFlags(
                        OTHER_FLAGS | Paint.STRIKE_THRU_TEXT_FLAG, false));
    }

    @Test
    public void testFlagsAreUnchangedWhenAlreadyCorrect() {
        Assert.assertEquals(OTHER_FLAGS, TableRowAdapter.strikeThroughFlags(OTHER_FLAGS, false));
        int struck = OTHER_FLAGS | Paint.STRIKE_THRU_TEXT_FLAG;
        Assert.assertEquals(struck, TableRowAdapter.strikeThroughFlags(struck, true));
    }
}
