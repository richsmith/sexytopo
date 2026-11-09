package org.hwyl.sexytopo.model.calibration;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class CalibrationReadingListTest {

    @Test
    public void testReplacingFromTheMiddleLeavesAGap() {
        CalibrationReadingList list = listOf(3);
        CalibrationReading third = list.getAll().get(2);

        list.replace(1);

        Assert.assertEquals(3, list.getAll().size());
        Assert.assertNull(list.getAll().get(1));
        Assert.assertSame(third, list.getAll().get(2));
        Assert.assertEquals(2, list.getCount());
        Assert.assertEquals(1, list.getNextIndex());
    }

    @Test
    public void testNextReadingFillsTheFirstGap() {
        CalibrationReadingList list = listOf(4);
        list.replace(2);
        list.replace(1);

        CalibrationReading retaken = new CalibrationReading();
        list.add(retaken);

        Assert.assertSame(retaken, list.getAll().get(1));
        Assert.assertEquals(2, list.getNextIndex());
        Assert.assertSame(retaken, list.getLastAdded());
    }

    @Test
    public void testReplacingTheEndLeavesNoTrailingGaps() {
        CalibrationReadingList list = listOf(3);
        list.replace(1);
        list.replace(2);

        Assert.assertEquals(1, list.getAll().size());
        Assert.assertEquals(1, list.getNextIndex());
    }

    @Test
    public void testDeletingMovesLaterReadingsUp() {
        CalibrationReadingList list = listOf(3);
        CalibrationReading third = list.getAll().get(2);

        list.delete(1);

        Assert.assertEquals(2, list.getAll().size());
        Assert.assertSame(third, list.getAll().get(1));
        Assert.assertEquals(2, list.getNextIndex());
    }

    @Test
    public void testDeletingAGapClosesIt() {
        CalibrationReadingList list = listOf(3);
        list.replace(1);

        list.delete(1);

        Assert.assertEquals(2, list.getAll().size());
        Assert.assertTrue(list.isComplete(2));
    }

    @Test
    public void testIsCompleteOnlyWithoutGaps() {
        CalibrationReadingList list = listOf(4);
        Assert.assertTrue(list.isComplete(4));
        Assert.assertTrue(list.isComplete(3));

        list.replace(0);
        Assert.assertFalse(list.isComplete(4));
        Assert.assertFalse(list.isComplete(1));
    }

    @Test
    public void testExtraReadingsStillNeedTheirGapsFilled() {
        CalibrationReadingList list = listOf(5);
        Assert.assertTrue(list.isComplete(4));

        list.replace(3);
        Assert.assertFalse(list.isComplete(2));
    }

    @Test
    public void testDeletingTheLastReadingShowsTheOneBefore() {
        CalibrationReadingList list = listOf(2);
        CalibrationReading first = list.getAll().get(0);

        list.deleteLastAdded();

        Assert.assertEquals(1, list.getCount());
        Assert.assertSame(first, list.getLastAdded());

        list.deleteLastAdded();
        Assert.assertNull(list.getLastAdded());
        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void testDeletingARetakeLeavesTheGapAgain() {
        CalibrationReadingList list = listOf(4);
        list.replace(1);
        list.add(new CalibrationReading());

        list.deleteLastAdded();

        Assert.assertEquals(4, list.getAll().size());
        Assert.assertNull(list.getAll().get(1));
        Assert.assertEquals(1, list.getNextIndex());
    }

    @Test
    public void testSetAllKeepsGapsButTrimsTheEnd() {
        CalibrationReadingList list = new CalibrationReadingList();
        CalibrationReading first = new CalibrationReading();
        CalibrationReading third = new CalibrationReading();

        list.setAll(Arrays.asList(first, null, third, null));

        Assert.assertEquals(3, list.getAll().size());
        Assert.assertEquals(1, list.getNextIndex());
        Assert.assertSame(third, list.getLastAdded());
    }

    private static CalibrationReadingList listOf(int count) {
        CalibrationReadingList list = new CalibrationReadingList();
        for (int i = 0; i < count; i++) {
            list.add(new CalibrationReading());
        }
        return list;
    }
}
