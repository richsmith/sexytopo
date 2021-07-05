package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class SketchJsonTranslater {

    public static final String PATHS_TAG = "paths";
    public static final String POINTS_TAG = "points";
    public static final String COLOUR_TAG = "colour";
    public static final String SYMBOLS_TAG = "symbols";
    public static final String LABELS_TAG = "labels";
    public static final String CROSS_SECTIONS_TAG = "x-sections";
    public static final String SYMBOL_ID_TAG = "symbol-id";
    public static final String TEXT_TAG = "text";
    public static final String SIZE_TAG = "size";
    public static final String STATION_ID_TAG = "station-id";
    public static final String POSITION_TAG = "location";
    public static final String ANGLE_TAG = "angle";
    public static final String X_TAG = "x";
    public static final String Y_TAG = "y";


    public static String translate(Sketch sketch) throws JSONException {
        return toJson(sketch).toString(SexyTopo.JSON_INDENT);
    }


    public static Sketch translate(Survey survey, String string) throws JSONException {
        JSONObject json = new JSONObject(string);
        return toSketch(survey, json);
    }


    public static synchronized JSONObject toJson(Sketch sketch) throws JSONException {

        JSONObject json = new JSONObject();

        JSONArray pathDetailArray = new JSONArray();
        for (PathDetail pathDetail : sketch.getPathDetails()) {
            pathDetailArray.put(toJson(pathDetail));
        }
        json.put(PATHS_TAG, pathDetailArray);

        JSONArray textDetailArray = new JSONArray();
        for (TextDetail textDetail : sketch.getTextDetails()) {
            textDetailArray.put(toJson(textDetail));
        }
        json.put(LABELS_TAG, textDetailArray);

        JSONArray symbolDetailArray = new JSONArray();
        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            symbolDetailArray.put(toJson(symbolDetail));
        }
        json.put(SYMBOLS_TAG, symbolDetailArray);

        JSONArray crossSectionDetailArray = new JSONArray();
        for (CrossSectionDetail crossSectionDetail : sketch.getCrossSectionDetails()) {
            crossSectionDetailArray.put(toJson(crossSectionDetail));
        }
        json.put(CROSS_SECTIONS_TAG, crossSectionDetailArray);

        return json;
    }

    public static Sketch toSketch(Survey survey, JSONObject json) {

        Sketch sketch = new Sketch();

        try {
            JSONArray pathsArray = json.getJSONArray(PATHS_TAG);
            List<PathDetail> pathDetails = new ArrayList<>();
            for (JSONObject object : Util.toList(pathsArray)) {
                pathDetails.add(toPathDetail(object));
            }
            sketch.setPathDetails(pathDetails);
        } catch (JSONException e) {
            Log.e("Failed to load sketch paths: " + e);
        }

        try {
            JSONArray symbolsArray = json.getJSONArray(SYMBOLS_TAG);
            List<SymbolDetail> symbolDetails = new ArrayList<>();
            for (JSONObject object : Util.toList(symbolsArray)) {
                symbolDetails.add(toSymbolDetail(object));
            }
            sketch.setSymbolDetails(symbolDetails);
        } catch (JSONException e) {
            Log.e("Failed to load symbols: " + e);
        }

        try {
            JSONArray labelsArray = json.getJSONArray(LABELS_TAG);
            List<TextDetail> textDetails = new ArrayList<>();
            for (JSONObject object : Util.toList(labelsArray)) {
                textDetails.add(toTextDetail(object));
            }
            sketch.setTextDetails(textDetails);
        } catch (JSONException e) {
            Log.e("Failed to load sketch labels: " + e);
        }

        try {
            JSONArray crossSectionsArray = json.getJSONArray(CROSS_SECTIONS_TAG);
            List<CrossSectionDetail> crossSectionDetails = new ArrayList<>();
            for (JSONObject object : Util.toList(crossSectionsArray)) {
                crossSectionDetails.add(toCrossSectionDetail(survey, object));
            }
            sketch.setCrossSectionDetails(crossSectionDetails);
        } catch (JSONException e) {
            Log.e("Failed to load cross-sections: " + e);
        }

        return sketch;
    }


    public static JSONObject toJson(PathDetail pathDetail) throws JSONException {

        JSONObject json = new JSONObject();
        json.put(COLOUR_TAG, pathDetail.getColour().toString());

        JSONArray points = new JSONArray();
        for (Coord2D coord : pathDetail.getPath()) {
            points.put(toJson(coord));
        }
        json.put(POINTS_TAG, points);

        return json;
    }


    public static PathDetail toPathDetail(JSONObject json) throws JSONException {

        Colour colour = Colour.valueOf(json.getString(COLOUR_TAG));

        JSONArray array = json.getJSONArray(POINTS_TAG);
        List<Coord2D> path = new ArrayList<>();
        for (JSONObject object : Util.toList(array)) {
            path.add(toCoord2D(object));
        }

        PathDetail pathDetail = new PathDetail(path, colour);

        double epsilon = Space2DUtils.simplificationEpsilon(pathDetail);
        List<Coord2D> simplifiedPath = Space2DUtils.simplify(path, epsilon);
        pathDetail.setPath(simplifiedPath);

        return pathDetail;
    }


    public static JSONObject toJson(SymbolDetail symbolDetail) throws JSONException {

        JSONObject json = new JSONObject();
        json.put(POSITION_TAG, toJson(symbolDetail.getPosition()));
        json.put(SYMBOL_ID_TAG, symbolDetail.getSymbol().toString());
        json.put(COLOUR_TAG, symbolDetail.getColour().toString());
        json.put(SIZE_TAG, symbolDetail.getSize());

        return json;
    }


    public static SymbolDetail toSymbolDetail(JSONObject json) throws JSONException {

        Colour colour = Colour.valueOf(json.getString(COLOUR_TAG));
        Coord2D location = toCoord2D(json.getJSONObject(POSITION_TAG));
        Symbol symbol = Symbol.valueOf(json.getString(SYMBOL_ID_TAG));

        float size = (float)(json.has(SIZE_TAG)? json.getDouble(SIZE_TAG) : 1);

        SymbolDetail symbolDetail = new SymbolDetail(location, symbol, colour, size);
        return symbolDetail;
    }


    public static JSONObject toJson(TextDetail textDetail) throws JSONException {

        JSONObject json = new JSONObject();
        json.put(POSITION_TAG, toJson(textDetail.getPosition()));
        json.put(TEXT_TAG, textDetail.getText());
        json.put(COLOUR_TAG, textDetail.getColour().toString());
        json.put(SIZE_TAG, textDetail.getSize());

        return json;
    }


    public static TextDetail toTextDetail(JSONObject json) throws JSONException {
        Colour colour = Colour.valueOf(json.getString(COLOUR_TAG));
        Coord2D location = toCoord2D(json.getJSONObject(POSITION_TAG));
        String text = json.getString(TEXT_TAG);
        float scale = (float)(json.has(SIZE_TAG)? json.getDouble(SIZE_TAG) : 0);
        TextDetail textDetail = new TextDetail(location, text, colour, scale);

        return textDetail;
    }


    public static JSONObject toJson(CrossSectionDetail crossSectionDetail) throws JSONException {

        JSONObject json = new JSONObject();
        json.put(STATION_ID_TAG, crossSectionDetail.getCrossSection().getStation().getName());
        json.put(POSITION_TAG, toJson(crossSectionDetail.getPosition()));
        json.put(ANGLE_TAG, crossSectionDetail.getCrossSection().getAngle());

        return json;
    }


    public static CrossSectionDetail toCrossSectionDetail(Survey survey, JSONObject json) throws JSONException {

        Coord2D position = toCoord2D(json.getJSONObject(POSITION_TAG));
        double angle = json.getDouble(ANGLE_TAG);

        String stationdId = json.getString(STATION_ID_TAG);
        Station station = survey.getStationByName(stationdId);

        CrossSectionDetail crossSectionDetail =
                new CrossSectionDetail(new CrossSection(station, angle), position);

        return crossSectionDetail;
    }


    public static JSONObject toJson(Coord2D coord) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(X_TAG, coord.x);
        json.put(Y_TAG, coord.y);
        return json;
    }


    public static Coord2D toCoord2D(JSONObject json) throws JSONException {
        return new Coord2D(json.getDouble(X_TAG), json.getDouble(Y_TAG));
    }


}
