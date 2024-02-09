package org.hwyl.sexytopo.demo;

import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.junit.Assert;

import org.junit.Test;


public class TestSurveyCreatorTest {

    @Test
    public void testCreateDoesNotThrowException() {
        ExampleSurveyCreator.create(10, 10);
        Assert.assertTrue(true);
    }

}
