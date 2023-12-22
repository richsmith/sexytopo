package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.AutoScalableDetail;
import org.hwyl.sexytopo.model.sketch.SinglePositionDetail;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PointTranslater {

    public static String export(SinglePositionDetail pointDetail) {

        String name;

        Coord2D position = pointDetail.getPosition();
        Float x = position.x;
        Float y = position.y;

        Map<String, String> commands = new HashMap<>();

        if (pointDetail instanceof SymbolDetail) {
            SymbolDetail symbolDetail = (SymbolDetail) pointDetail;
            Symbol symbol = symbolDetail.getSymbol();
            name = symbol.getTherionName();
            if (symbol.isDirectional()) {
                commands.put("orientation", Float.toString(symbolDetail.getAngle()));
            }

        } else if (pointDetail instanceof TextDetail) {
            TextDetail textDetail = (TextDetail) pointDetail;
            name = "label";
            commands.put("text", textDetail.getText());

        } else {
            throw new RuntimeException("Unknown sketch detail type");
        }

        if (pointDetail instanceof AutoScalableDetail) {
            String scale = getScale((AutoScalableDetail) pointDetail);
            commands.put("scale", scale);
        }

        List<String> tokens = new ArrayList<>();
        tokens.add("point");
        tokens.add(x.toString());
        tokens.add(y.toString());
        tokens.add(name);

        for (Map.Entry<String, String> entry: commands.entrySet()) {
            tokens.add("-" + entry.getKey());
            tokens.add(entry.getValue());
        }

        String line = TextTools.join(" ", tokens);
        return line;
    }

    private static String getScale(AutoScalableDetail detail) {

        float sizeInMetres = detail.getSize();

        // These numbers were determined by creating some stal in Therion
        // at different scales and seeing how big they came out

        // Therion seems to have a small number of sizes available; not sure
        // if there's any way of handling very big symbols

        if (sizeInMetres < 0.45) {
            return "xs"; // appprox 0.4m
        } else if (sizeInMetres < 0.6) {
            return "s"; // approx 0.5m
        } else if (sizeInMetres < 0.9) {
            return "m"; // approx 0.7m
        } else if (sizeInMetres < 1.3) {
            return "l"; // approx 1.1m
        } else {
            return "xl"; // approx 1.5m
        }
    }
}
