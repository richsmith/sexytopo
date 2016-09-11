package org.hwyl.sexytopo.control.io.therion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TherionImporter {

    public void toSurveyAndTemplate(String text) {




    }


    public static void getCentreline(String text) throws Exception {
        List<String> lines = Arrays.asList(text.split("\n"));

        List<String> block = getContentsOfBeginEndBlock(lines, "centreline");

    }

    public static List<String> getContentsOfBeginEndBlock(List<String> lines, String tag)
            throws Exception {

        boolean foundStartBlock = false;
        boolean foundEndBlock = false;
        String endTag = "end" + tag;

        List<String> contents = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();

            if (line.equals(tag)) {
                if (foundStartBlock) {
                    throw new Exception("Opening block tag " + tag + " encountered twice");
                } else {
                    foundStartBlock = true;
                    continue;
                }

            } else if (line.equals(endTag)) {
                if (foundStartBlock) {
                    foundEndBlock = true;
                    break;
                } else {
                    throw new Exception("End block tag " + endTag + " encountered before block start");
                }
            }

            contents.add(line);
        }

        if (!foundStartBlock) {
            throw new Exception("Failed to find opening block tag " + tag);
        } else if (!foundEndBlock) {
            throw new Exception("Failed to find end block tag " + endTag);
        }

        return contents;
    }

}
