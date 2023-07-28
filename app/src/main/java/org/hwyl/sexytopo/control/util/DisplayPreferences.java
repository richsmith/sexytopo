package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.sketch.Symbol;


public class DisplayPreferences {

    public static final String SKETCH_PREFERENCES_KEY = "display";

    private static final String SKETCH_TOOL_PREFERENCE_KEY = "pref_sketch_sketch_tool";
    private static final String BRUSH_COLOUR_PREFERENCE_KEY = "pref_sketch_brush_colour";
    private static final String SYMBOL_PREFERENCE_KEY = "pref_sketch_symbol";


    /**
     * Toggle-able display options set throught the sketch pop-up menu.
     * We have a lot of these, so easier to use an enum than squillions of get/sets.
     */
    public enum Toggle {
        AUTO_RECENTRE(R.id.buttonAutoRecentre, false),
        SNAP_TO_LINES(R.id.buttonSnapToLines, false),
        FADE_NON_ACTIVE(R.id.buttonFadeNonActive, false),
        SHOW_GRID(R.id.buttonShowGrid, true),
        SHOW_SPLAYS(R.id.buttonShowSplays, true),
        SHOW_SKETCH(R.id.buttonShowSketch, true),
        SHOW_STATION_LABELS(R.id.buttonShowStationLabels, true),
        SHOW_CONNECTIONS(R.id.buttonShowConnections, true);

        private final int controlId;
        private final boolean defaultValue;

        Toggle(int id, boolean defaultValue) {
            this.controlId = id;
            this.defaultValue = defaultValue;
        }

        public void set(boolean value) {
            setBoolean(this.toString(), value);
        }

        public Boolean isOn() {
            return prefs.getBoolean(this.toString(), defaultValue);
        }

        public int getControlId() {
            return controlId;
        }
    }


    private static SharedPreferences prefs;

    public static void initialise(Context context) {
        prefs = context.getSharedPreferences(SKETCH_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    private static void setString(String id, String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(id, value);
        editor.apply();
    }

    private static void setBoolean(String id, Boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(id, value);
        editor.apply();
    }

    // ********** Tools ***********

    public static void setSelectedSketchTool(SketchTool tool) {
        setString(SKETCH_TOOL_PREFERENCE_KEY, tool.toString());
    }

    public static SketchTool getSelectedSketchTool() {
        String selected = prefs.getString(SKETCH_TOOL_PREFERENCE_KEY, null);
        SketchTool tool = SketchTool.fromString(selected);
        return tool;
    }

    public static void setSelectedBrushColour(BrushColour brushColour) {
        setString(BRUSH_COLOUR_PREFERENCE_KEY, brushColour.toString());
    }

    public static BrushColour getSelectedBrushColour() {
        String selected = prefs.getString(BRUSH_COLOUR_PREFERENCE_KEY, null);
        BrushColour brushColour = BrushColour.fromString(selected);
        return brushColour;
    }

    public static void setSelectedSymbol(Symbol symbol) {
        setString(SYMBOL_PREFERENCE_KEY, symbol.toString());
    }

    public static Symbol getSelectedSymbol() {
        String selected = prefs.getString(SYMBOL_PREFERENCE_KEY, null);
        Symbol symbol = Symbol.fromString(selected);
        return symbol;
    }

}
