package org.hwyl.sexytopo.control.io.translation;

import org.apache.commons.io.FilenameUtils;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;


public abstract class Importer {

    public abstract Survey toSurvey(File file) throws Exception ;

    public abstract boolean canHandleFile(File file);

    public String getDefaultName(File file) {
        return FilenameUtils.removeExtension(file.getName());
    }
}
