package org.hwyl.sexytopo.control.io;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rls on 15/02/15.
 */
public class SketchJsonTranslater {

    public static final String PATHS_TAG = "paths";
    public static final String POINTS_TAG = "points";
    public static final String COLOUR_TAG = "colour";
    public static final String X_TAG = "x";
    public static final String Y_TAG = "y";


    public static String translate(Sketch sketch) throws JSONException {
        return toJson(sketch).toString();
    }


    public static Sketch translate(String string) throws JSONException {
        JSONObject json = new JSONObject(string);
        return toSketch(json);
    }


    public static JSONObject toJson(Sketch sketch) throws JSONException {

        JSONObject json = new JSONObject();

        JSONArray array = new JSONArray();
        for (PathDetail pathDetail : sketch.getPathDetails()) {
            array.put(toJson(pathDetail));
        }

        json.put(PATHS_TAG, array);

        return json;
    }

    public static Sketch toSketch(JSONObject json) throws JSONException {
        JSONArray array = json.getJSONArray(PATHS_TAG);
        List<PathDetail> pathDetails = new ArrayList<>();
        for (JSONObject object : toList(array)) {
            pathDetails.add(toPathDetail(object));
        }

        Sketch sketch = new Sketch();
        sketch.setPathDetails(pathDetails);
        return sketch;
    }


    public static JSONObject toJson(PathDetail pathDetail) throws JSONException {

        JSONObject json = new JSONObject();
        json.put(COLOUR_TAG, pathDetail.getColour());

        JSONArray points = new JSONArray();
        for (Coord2D coord : pathDetail.getPath()) {
            points.put(toJson(coord));
        }
        json.put(POINTS_TAG, points);

        return json;
    }


    public static PathDetail toPathDetail(JSONObject json) throws JSONException {

        int colour = json.getInt(COLOUR_TAG);

        JSONArray array = json.getJSONArray(POINTS_TAG);
        List<Coord2D> path = new ArrayList<>();
        for (JSONObject object : toList(array)) {
            path.add(toCoord2D(object));
        }

        PathDetail pathDetail = new PathDetail(path, colour);
        return pathDetail;
    }


    public static JSONObject toJson(Coord2D coord) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(X_TAG, coord.getX());
        json.put(Y_TAG, coord.getY());
        return json;
    }


    public static Coord2D toCoord2D(JSONObject json) throws JSONException {
        return new Coord2D(json.getDouble(X_TAG), json.getDouble(Y_TAG));
    }


    private static List<JSONObject> toList(JSONArray array) throws JSONException {
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getJSONObject(i));
        }
        return list;
    }
}
