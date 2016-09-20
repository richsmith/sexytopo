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

}
