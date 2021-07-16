package org.hwyl.sexytopo.demo;

import org.junit.Assert;

import org.junit.Test;


public class TestSurveyCreatorTest {

    @Test
    public void testCreateDoesNotThrowException() {
        TestSurveyCreator.create("TestSurvey", 10, 10);
        Assert.assertTrue(true);
    }

}
