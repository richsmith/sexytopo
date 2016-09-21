package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;


public class XviImporter implements Importer {

    public Survey toSurvey(File file) {
        Survey survey = new Survey(file.getName());
        String contents = Loader.slurpFile(file);
        Sketch sketch = getSketch(contents);
        survey.setPlanSketch(sketch);
        return survey;
    }

    public static Sketch getSketch(String contents) {
        Sketch sketch = new Sketch();
        return sketch;
    }

    public boolean canHandleFile(File file) {
        return file.getName().endsWith("xvi");
    }

    public static String getBlockContents(String text, String command) throws Exception {

        outer:
        for (int i = 0; i < text.length(); i++) {

            // keep trying to match the command string
            for (int j = 0; j < command.length(); j++) {
                char text_current = text.charAt(i + j);
                char command_current = command.charAt(j);
                if (text_current != command_current) {
                    continue outer;
                }
            }

            i += command.length();
            // from here we're at the end of a matched command string

            // eat up any whitespace
            while (Character.isWhitespace(text.charAt(i))) {
                i++;
            }

            // hopefully we've found an opening { block now
            if (text.charAt(i) == '{') {
                i++;
            } else {
                continue outer; // otherwise try to match again...
            }

            int openBraces = 1;
            int startOfContent = i;

            for(; i < text.length(); i++) {
                if (text.charAt(i) == '{') {
                    openBraces++;
                } else if (text.charAt(i) == '}') {
                    openBraces--;
                }

                if (openBraces == 0) {
                    int endOfContent = i;
                    String content = text.substring(startOfContent, endOfContent);
                    return content;
                }

            }

            throw new Exception("Malformed text: could not match braces");

        }

        throw new Exception("Could not match command in text");
    }

}
