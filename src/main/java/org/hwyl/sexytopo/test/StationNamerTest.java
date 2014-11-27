package org.hwyl.sexytopo.test;

import android.test.InstrumentationTestCase;

import org.hwyl.sexytopo.control.util.StationNamer;
import org.hwyl.sexytopo.model.Leg;
import org.hwyl.sexytopo.model.Station;
import org.hwyl.sexytopo.model.Survey;

/**
 * Created by rls on 26/07/14.
 */
public class StationNamerTest extends InstrumentationTestCase {

    public void testAdvanceLastNumber() {
        String advanced = StationNamer.advanceLastNumber("S1");
        assertEquals("S2", advanced);
    }

    public void testAdvanceLastNumber2() {
        String advanced = StationNamer.advanceLastNumber("S2-1.1");
        assertEquals("S2-1.2", advanced);
    }

    public void testGenerateNextBranch() {
        String nextBranchName = StationNamer.generateNextBranch("S1", 1);
        assertEquals("S1-1.1", nextBranchName);
    }

    public void testGenerateNextBranch2() {
        String nextBranchName = StationNamer.generateNextBranch("S1-1.1", 1);
        assertEquals("S1-1.1-1.1", nextBranchName);
    }

    public void testGenerateNextStationName() {
        Station testStation = createTestStation();
        String nextName = StationNamer.generateNextStationName(testStation);
        assertEquals("S1-1.1", nextName);
    }


    private Station createTestStation() {
        Station s1 = new Station(StationNamer.generateOriginName());
        Station s2 = new Station(StationNamer.advanceLastNumber(s1.getName()));

        Leg leg = new Leg(0, 0, 0, s2);
        s1.addOnwardLeg(leg);

        return s1;
    }

}
