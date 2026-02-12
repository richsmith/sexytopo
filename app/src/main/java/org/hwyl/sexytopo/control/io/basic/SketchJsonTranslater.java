package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchLayer;
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


@SuppressWarnings("UnnecessaryLocalVariable")
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

    // Layer-related tags
    public static final String LAYERS_TAG = "layers";
    public static final String LAYER_ID_TAG = "id";
    public static final String LAYER_NAME_TAG = "name";
    public static final String LAYER_VISIBILITY_TAG = "visibility";
    public static final String CROSS_SECTION_SKETCH_TAG = "sketch";
    public static final String ACTIVE_LAYER_ID_TAG = "activeLayerId";


    public static String translate(Sketch sketch) throws JSONException {
        return toJson(sketch).toString(SexyTopoConstants.JSON_INDENT);
    }


    public static Sketch translate(Survey survey, String string) throws JSONException {
        JSONObject json = new JSONObject(string);
        return toSketch(survey, json);
    }


    public static synchronized JSONObject toJson(Sketch sketch) throws JSONException {

        JSONObject json = new JSONObject();

        // Save layers
        JSONArray layersArray = new JSONArray();
        for (SketchLayer layer : sketch.getLayers()) {
            layersArray.put(toJson(layer));
        }
        json.put(LAYERS_TAG, layersArray);
        json.put(ACTIVE_LAYER_ID_TAG, sketch.getActiveLayerId());

        return json;
    }

    public static JSONObject toJson(SketchLayer layer) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(LAYER_ID_TAG, layer.getId());
        json.put(LAYER_NAME_TAG, layer.getName());
        json.put(LAYER_VISIBILITY_TAG, layer.getVisibility().toString());

        JSONArray pathDetailArray = new JSONArray();
        for (PathDetail pathDetail : layer.getPathDetails()) {
            pathDetailArray.put(toJson(pathDetail));
        }
        json.put(PATHS_TAG, pathDetailArray);

        JSONArray textDetailArray = new JSONArray();
        for (TextDetail textDetail : layer.getTextDetails()) {
            textDetailArray.put(toJson(textDetail));
        }
        json.put(LABELS_TAG, textDetailArray);

        JSONArray symbolDetailArray = new JSONArray();
        for (SymbolDetail symbolDetail : layer.getSymbolDetails()) {
            symbolDetailArray.put(toJson(symbolDetail));
        }
        json.put(SYMBOLS_TAG, symbolDetailArray);

        JSONArray crossSectionDetailArray = new JSONArray();
        for (CrossSectionDetail crossSectionDetail : layer.getCrossSectionDetails()) {
            crossSectionDetailArray.put(toJson(crossSectionDetail));
        }
        json.put(CROSS_SECTIONS_TAG, crossSectionDetailArray);

        return json;
    }

    public static Sketch toSketch(Survey survey, JSONObject json) {

        Sketch sketch = new Sketch();

        // Check if this is the new layer format or legacy format
        if (json.has(LAYERS_TAG)) {
            // New layer format
            try {
                sketch.getLayers().clear(); // Remove default layer
                JSONArray layersArray = json.getJSONArray(LAYERS_TAG);
                List<SketchLayer> layers = new ArrayList<>();
                for (JSONObject layerJson : IoUtils.toList(layersArray)) {
                    layers.add(toSketchLayer(survey, layerJson));
                }
                sketch.setLayers(layers);

                if (json.has(ACTIVE_LAYER_ID_TAG)) {
                    int activeLayerId = json.getInt(ACTIVE_LAYER_ID_TAG);
                    // Set directly without triggering undo history
                    for (SketchLayer layer : sketch.getLayers()) {
                        if (layer.getId() == activeLayerId) {
                            sketch.setActiveLayerId(activeLayerId);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(R.string.file_load_sketch_paths_error, e);
            }
        } else {
            // Legacy format - load into default layer
            loadLegacyFormat(survey, json, sketch.getActiveLayer());
        }

        return sketch;
    }

    private static void loadLegacyFormat(Survey survey, JSONObject json, SketchLayer layer) {
        try {
            JSONArray pathsArray = json.getJSONArray(PATHS_TAG);
            List<PathDetail> pathDetails = new ArrayList<>();
            for (JSONObject object : IoUtils.toList(pathsArray)) {
                pathDetails.add(toPathDetail(object));
            }
            layer.setPathDetails(pathDetails);
        } catch (Exception e) {
            Log.e(R.string.file_load_sketch_paths_error, e);
        }

        try {
            JSONArray symbolsArray = json.getJSONArray(SYMBOLS_TAG);
            List<SymbolDetail> symbolDetails = new ArrayList<>();
            for (JSONObject object : IoUtils.toList(symbolsArray)) {
                try {
                    symbolDetails.add(toSymbolDetail(object));
                } catch (Exception e) {
                    Log.i(R.string.file_load_symbols_error, e);
                }
            }
            layer.setSymbolDetails(symbolDetails);
        } catch (Exception e) {
            Log.e(R.string.file_load_symbols_error, e);
        }

        try {
            JSONArray labelsArray = json.getJSONArray(LABELS_TAG);
            List<TextDetail> textDetails = new ArrayList<>();
            for (JSONObject object : IoUtils.toList(labelsArray)) {
                textDetails.add(toTextDetail(object));
            }
            layer.setTextDetails(textDetails);
        } catch (Exception e) {
            Log.e(R.string.file_load_sketch_labels_error, e);
        }

        try {
            JSONArray crossSectionsArray = json.getJSONArray(CROSS_SECTIONS_TAG);
            List<CrossSectionDetail> crossSectionDetails = new ArrayList<>();
            for (JSONObject object : IoUtils.toList(crossSectionsArray)) {
                crossSectionDetails.add(toCrossSectionDetail(survey, object));
            }
            layer.setCrossSectionDetails(crossSectionDetails);
        } catch (Exception e) {
            Log.e(R.string.file_load_cross_sections_error, e);
        }
    }

    public static SketchLayer toSketchLayer(Survey survey, JSONObject json) throws JSONException {
        int id = json.getInt(LAYER_ID_TAG);
        String name = json.getString(LAYER_NAME_TAG);
        SketchLayer layer = new SketchLayer(id, name);

        if (json.has(LAYER_VISIBILITY_TAG)) {
            String visibilityStr = json.getString(LAYER_VISIBILITY_TAG);
            layer.setVisibility(SketchLayer.Visibility.valueOf(visibilityStr));
        }

        // Load layer content using legacy format loader
        loadLegacyFormat(survey, json, layer);

        return layer;
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
        for (JSONObject object : IoUtils.toList(array)) {
            path.add(toCoord2D(object));
        }

        PathDetail pathDetail = new PathDetail(path, colour);

        float epsilon = Space2DUtils.simplificationEpsilon(pathDetail);
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

        if (symbolDetail.getAngle() != 0) {
            json.put(ANGLE_TAG, symbolDetail.getAngle());
        }

        return json;
    }


    public static SymbolDetail toSymbolDetail(JSONObject json) throws JSONException {

        Colour colour = Colour.valueOf(json.getString(COLOUR_TAG));
        Coord2D location = toCoord2D(json.getJSONObject(POSITION_TAG));
        Symbol symbol = Symbol.valueOf(json.getString(SYMBOL_ID_TAG));

        float size = (float)(json.has(SIZE_TAG)? json.getDouble(SIZE_TAG) : 1);
        float angle = (float)(json.has(ANGLE_TAG)? json.getDouble(ANGLE_TAG) : 0);

        SymbolDetail symbolDetail = new SymbolDetail(location, symbol, colour, size, angle);
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
        
        // Save the cross-section's sketch (only the first layer's paths for simplicity)
        Sketch xsSketch = crossSectionDetail.getCrossSection().getSketch();
        if (xsSketch != null && !xsSketch.getPathDetails().isEmpty()) {
            JSONObject sketchJson = new JSONObject();
            JSONArray pathsArray = new JSONArray();
            for (PathDetail path : xsSketch.getPathDetails()) {
                pathsArray.put(toJson(path));
            }
            sketchJson.put(PATHS_TAG, pathsArray);
            json.put(CROSS_SECTION_SKETCH_TAG, sketchJson);
        }

        return json;
    }


    public static CrossSectionDetail toCrossSectionDetail(Survey survey, JSONObject json)
            throws JSONException {

        Coord2D position = toCoord2D(json.getJSONObject(POSITION_TAG));
        float angle = (float) json.getDouble(ANGLE_TAG);

        String stationdId = json.getString(STATION_ID_TAG);
        Station station = survey.getStationByName(stationdId);

        CrossSection crossSection = new CrossSection(station, angle);
        
        // Load the cross-section's sketch if present
        if (json.has(CROSS_SECTION_SKETCH_TAG)) {
            JSONObject sketchJson = json.getJSONObject(CROSS_SECTION_SKETCH_TAG);
            if (sketchJson.has(PATHS_TAG)) {
                JSONArray pathsArray = sketchJson.getJSONArray(PATHS_TAG);
                List<PathDetail> paths = new ArrayList<>();
                for (JSONObject pathObj : IoUtils.toList(pathsArray)) {
                    paths.add(toPathDetail(pathObj));
                }
                crossSection.getSketch().setPathDetails(paths);
            }
        }
        
        CrossSectionDetail crossSectionDetail = new CrossSectionDetail(crossSection, position);

        return crossSectionDetail;
    }


    public static JSONObject toJson(Coord2D coord) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(X_TAG, coord.x);
        json.put(Y_TAG, coord.y);
        return json;
    }


    public static Coord2D toCoord2D(JSONObject json) throws JSONException {
        return new Coord2D((float)json.getDouble(X_TAG), (float)json.getDouble(Y_TAG));
    }


}
