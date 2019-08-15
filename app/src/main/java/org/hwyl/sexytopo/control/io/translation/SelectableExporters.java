package org.hwyl.sexytopo.control.io.translation;

import org.hwyl.sexytopo.control.io.thirdparty.compass.CompassExporter;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexExporter;
import org.hwyl.sexytopo.control.io.thirdparty.svg.SvgExporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionExporter;

import java.util.Arrays;
import java.util.List;


public class SelectableExporters {

    public static final List<? extends Exporter> EXPORTERS = Arrays.asList(
        new TherionExporter(),
        new SurvexExporter(),
        // new PocketTopoTxtExporter(), <- disabled due to several issues
        new CompassExporter(),
        new SvgExporter()
    );

}
