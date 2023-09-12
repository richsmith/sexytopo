package org.hwyl.sexytopo.model.sketch;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public enum Symbol {


    // ********** Special **********
    TEXT(R.string.symbol_entrance, R.drawable.text, "text.svg", false),

    // ********** Cave geometry **********
    ENTRANCE(R.string.symbol_entrance, R.drawable.symbol_uis_entrance, "symbol_uis_entrance.svg", true),
    GRADIENT(R.string.symbol_gradient, R.drawable.symbol_uis_gradient, "symbol_uis_gradient.svg", true),
    TOO_TIGHT(R.string.symbol_too_tight, R.drawable.symbol_uis_too_tight, "symbol_uis_too_tight.svg", true),

    // ********** Floor stuff **********
    SAND(R.string.symbol_sand, R.drawable.symbol_uis_sand, "symbol_uis_sand.svg", false),
    CLAY(R.string.symbol_clay, R.drawable.symbol_uis_clay, "symbol_uis_clay.svg", false),
    PEBBLES(R.string.symbol_pebbles, R.drawable.symbol_uis_pebbles, "symbol_uis_pebbles.svg", false),
    BLOCKS(R.string.symbol_blocks, R.drawable.symbol_uis_blocks, "symbol_uis_blocks.svg", false),

    // ********** Speleothems **********
    STALACTITE(R.string.symbol_stalactite, R.drawable.symbol_uis_stalactite, "symbol_uis_stalactite.svg", false),
    STALAGMITE(R.string.symbol_stalagmite, R.drawable.symbol_uis_stalagmite, "symbol_uis_stalagmite.svg", false),
    COLUMN(R.string.symbol_column, R.drawable.symbol_uis_column, "symbol_uis_column.svg", false),
    CURTAIN(R.string.symbol_curtain, R.drawable.symbol_uis_curtain, "symbol_uis_curtain.svg", false),
    STRAWS(R.string.symbol_straws, R.drawable.symbol_uis_straws, "symbol_uis_straws.svg", false),
    HELICTITES(R.string.symbol_helictites, R.drawable.symbol_uis_helictite, "symbol_uis_helictite.svg", false),
    CRYSTALS(R.string.symbol_crystals, R.drawable.symbol_uis_crystal, "symbol_uis_crystal.svg", false),
    GOUR(R.string.symbol_gour, R.drawable.symbol_uis_gour, "symbol_uis_gour.svg", true),

    // ********** Fluids **********
    WATER_FLOW(R.string.symbol_water_flow, R.drawable.symbol_uis_flow, "symbol_uis_flow.svg", true),
    AIR_DRAUGHT(R.string.symbol_air_draught, R.drawable.symbol_uis_air_draught, "symbol_uis_air_draught.svg", true),

    // ********** Other **********
    GUANO(R.string.symbol_guano, R.drawable.symbol_uis_guano, "symbol_uis_guano.svg", false),
    DEBRIS(R.string.symbol_debris, R.drawable.symbol_uis_debris, "symbol_uis_debris.svg", false);

    private static Resources resources;

    private final int viewId = getViewId();
    private final int stringId;
    private final int drawableId;
    private final String svgFilename;
    private String rawSvg;

    private final boolean isDirectional;


    private static final Symbol DEFAULT = TEXT;

    public static final String SVG_DIR = "svg/symbols/";

    Symbol(int stringId, int drawableId, String svgFilename, boolean isDirectional) {
        this.stringId = stringId;
        this.drawableId = drawableId;
        this.svgFilename = svgFilename;
        this.isDirectional = isDirectional;
    }

    public static void setResources(Resources resources) {
        Symbol.resources = resources;
    }

    public static Symbol fromString(String name) {
        return name == null? DEFAULT : Symbol.valueOf(name);
    }


    public int getViewId() {
        return viewId;
    }

    public String getName() {
        return resources == null? this.toString() : resources.getString(stringId);
    }

    public boolean isDirectional() {
        return isDirectional;
    }

    public String asRawSvg() {
        if (rawSvg == null) {
            try {
                String path = SVG_DIR + svgFilename;
                rawSvg = readSvg(path);
            } catch (Exception exception) {
                Log.e(R.string.setup_error_load_symbol_svg, exception);
                rawSvg = "";
            }
        }

        return rawSvg;
    }
    private String readSvg(String path) throws IOException {
        AssetManager assetManager = resources.getAssets();
        InputStream inputStream;
        inputStream = assetManager.open(path);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }

    public Drawable createDrawable() {
        return ResourcesCompat.getDrawable(resources, drawableId, null).mutate();
    }

    public static Symbol getDefault() {
        return DEFAULT;
    }


}
