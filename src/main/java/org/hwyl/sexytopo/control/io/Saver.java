package org.hwyl.sexytopo.control.io;

import android.content.Context;

import org.hwyl.sexytopo.model.Survey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by rls on 03/08/14.
 */
public class Saver {


    public static void save(Context context, Survey survey) throws IOException {

        String surveyText = SurvexExporter.export(survey);
        Util.ensureDirectoryExists(Util.getDirectoryForSurveyFile(survey.getName()));
        String filename = Util.getPathForSurveyFile(survey.getName(), "svx");
        saveFile(context, filename, surveyText);

    }


    public static void saveFile(Context context, String filename, String contents) throws IOException {

        Util.ensureSurveyDirectoryExists();
        Util.ensureDirectoriesInPathExist(filename);


        File surveyFile = new File(filename);


        FileOutputStream stream = new FileOutputStream(surveyFile);

        try {
            stream.write(contents.getBytes());
        } finally {
            stream.close();
        }
    }





}
