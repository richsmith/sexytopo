package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;


public abstract class Exporter {

    public abstract void export(Survey survey) throws IOException;

    public abstract String getExportTypeName(Context context);

}
