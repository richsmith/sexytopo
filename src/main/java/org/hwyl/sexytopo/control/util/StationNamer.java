package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.Station;

/**
 * Created by rls on 24/07/14.
 */
public class StationNamer {

    public static final String BRANCH_SEPARATOR = "B";
    public static final String STATION_NUMBER_SEPARATOR = ".";
    public static final String PREFIX = "S";

    public static String generateOriginName() {
        return PREFIX + Integer.toString(1);
    }

    public static String generateNextStationName(Station originatingStation) {

        int numberOfExistingConnections = originatingStation.getConnectedOnwardLegs().size();

        String originatingName = originatingStation.getName();

        if (numberOfExistingConnections == 0) {
            return advanceLastNumber(originatingName);
        } else {
            return generateNextBranch(originatingName, numberOfExistingConnections);
        }
    }


    public static String advanceLastNumber(String originatingName) {

        String firstPartOfString = "";
        String lastNumberString = "";


        for (int i = originatingName.length() - 1; i >= 0; i--) {
            char c = originatingName.charAt(i);
            if (! Character.isDigit(c)) {
                firstPartOfString = originatingName.substring(0, i + 1);
                lastNumberString = originatingName.substring(i + 1);
                break;
            }
        }

        int value = Integer.parseInt(lastNumberString);
        return firstPartOfString + ++value;
    }


    public static String generateNextBranch(String originatingName, int branchNumber) {
        return originatingName + BRANCH_SEPARATOR + branchNumber + STATION_NUMBER_SEPARATOR + 1;
    }

}
