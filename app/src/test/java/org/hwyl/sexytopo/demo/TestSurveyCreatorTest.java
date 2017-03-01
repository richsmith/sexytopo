package org.hwyl.sexytopo.demo;

import junit.framework.Assert;

import org.junit.Test;


public class TestSurveyCreatorTest {

    @Test
    public void testCreateDoesNotThrowException() {
        TestSurveyCreator.create(10, 10);
        Assert.assertTrue(true);
    }

}
