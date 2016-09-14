package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TherionImporter implements Importer {

    public Survey toSurvey(File file) {

        Survey survey = new Survey(file.getName());
        return survey;
    }


    public boolean canHandleFile(File file) {
        return file.getName().endsWith("zip");
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
