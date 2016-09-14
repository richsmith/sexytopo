package org.hwyl.sexytopo.control.io.translation;

import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;


public interface Importer {

    public Survey toSurvey(File file) throws Exception ;

    public boolean canHandleFile(File file);
}
