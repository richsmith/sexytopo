package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.model.survey.Survey;


public class TherionExporter implements Exporter {


    public String export(Survey survey) {

        return "Error";
    }


    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_therion);
    }

    public static String getEncodingText() {
        return "encoding utf-8";
    }

}
