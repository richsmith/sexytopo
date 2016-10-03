package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.model.survey.Survey;


public interface Exporter {
    public String export(Survey survey);

    public String getExportTypeName(Context context);
}
