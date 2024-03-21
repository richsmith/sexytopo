package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import org.hwyl.sexytopo.control.io.thirdparty.compass.CompassExporter;
import org.hwyl.sexytopo.control.io.thirdparty.pockettopo.PocketTopoTxtExporter;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexExporter;
import org.hwyl.sexytopo.control.io.thirdparty.svg.SvgExporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionExporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SelectableExporters {

    public static final List<? extends Exporter> EXPORTERS = Arrays.asList(
        new TherionExporter(),
        new SurvexExporter(),
        new SvgExporter(),
        new PocketTopoTxtExporter(),
        new CompassExporter()
    );

    public static Exporter fromName(Context context, String name) {
        for (Exporter exporter: EXPORTERS) {
            if (exporter.getExportTypeDescription(context).equals(name)) {
                return exporter;
            }
        }
        return null;
    }

    public static List<String> getExportTypeNames(Context context) {
        List<String> names = new ArrayList<>();
        for (Exporter exporter: EXPORTERS) {
            names.add(exporter.getExportTypeDescription(context));
        }
        return names;
    }
}
