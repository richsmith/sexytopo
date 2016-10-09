package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.io.IOException;


public abstract class Exporter {

    public abstract void export(Survey survey) throws IOException;

    public abstract String getExportTypeName(Context context);

    public abstract String getExportDirectoryName();


    protected void saveToExportDirectory(Survey survey, String filename, String content)
            throws IOException {

        String directoryPath = Util.getExportDirectoryPath(getExportDirectoryName(), survey.getName());
        Util.ensureDirectoriesInPathExist(directoryPath);

        String path = directoryPath + File.separator + filename;
        Saver.saveFile(path, content);
    }

}
