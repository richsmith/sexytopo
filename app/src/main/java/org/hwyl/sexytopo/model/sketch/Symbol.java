package org.hwyl.sexytopo.model.sketch;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import org.hwyl.sexytopo.R;


public enum Symbol {

    // ********** Special **********
    TEXT(R.string.symbol_entrance, R.drawable.text, false),

    // ********** Cave geometry **********
    ENTRANCE(R.string.symbol_entrance, R.drawable.symbol_uis_entrance, true),
    GRADIENT(R.string.symbol_gradient, R.drawable.symbol_uis_gradient, true),
    TOO_TIGHT(R.string.symbol_too_tight, R.drawable.symbol_uis_too_tight, true),

    // ********** Floor stuff **********
    SAND(R.string.symbol_sand, R.drawable.symbol_uis_sand, false),
    CLAY(R.string.symbol_clay, R.drawable.symbol_uis_clay, false),
    PEBBLES(R.string.symbol_pebbles, R.drawable.symbol_uis_pebbles, false),
    BLOCKS(R.string.symbol_blocks, R.drawable.symbol_uis_blocks, false),

    // ********** Speleothems **********
    STALACTITE(R.string.symbol_stalactite, R.drawable.symbol_uis_stalactite, false),
    STALAGMITE(R.string.symbol_stalagmite, R.drawable.symbol_uis_stalagmite, false),
    COLUMN(R.string.symbol_column, R.drawable.symbol_uis_column, false),
    CURTAIN(R.string.symbol_curtain, R.drawable.symbol_uis_curtain, false),
    STRAWS(R.string.symbol_straws, R.drawable.symbol_uis_straws, false),
    HELICTITES(R.string.symbol_helictites, R.drawable.symbol_uis_helictite, false),
    CRYSTALS(R.string.symbol_crystals, R.drawable.symbol_uis_crystal, false),
    GOUR(R.string.symbol_gour, R.drawable.symbol_uis_gour, true),

    // ********** Fluids **********
    WATER_FLOW(R.string.symbol_water_flow, R.drawable.symbol_uis_flow, true),
    AIR_DRAUGHT(R.string.symbol_air_draught, R.drawable.symbol_uis_air_draught, true),

    // ********** Other **********
    GUANO(R.string.symbol_guano, R.drawable.symbol_uis_guano, false),
    DEBRIS(R.string.symbol_debris, R.drawable.symbol_uis_debris, false);

    private static Resources resources;

    private final int stringId;
    private final int drawableId;

    private final boolean isDirectional;


    private static final Symbol DEFAULT = STALACTITE;

    Symbol(int stringId, int drawableId, boolean isDirectional) {
        this.stringId = stringId;
        this.drawableId = drawableId;
        this.isDirectional = isDirectional;
    }

    public static void setResources(Resources resources) {
        Symbol.resources = resources;
    }

    public static Symbol fromString(String name) {
        return name == null? DEFAULT : Symbol.valueOf(name);
    }

    public String getName() {
        return resources == null? this.toString() : resources.getString(stringId);
    }

    public int getDrawableId() {
        return drawableId;
    }

    public boolean isDirectional() {
        return isDirectional;
    }

    public int getViewId() {
        return 4629347 + getDrawableId();
    }

    public Drawable createDrawable() {
        return ResourcesCompat.getDrawable(resources, drawableId, null).mutate();
    }

    public static Symbol getDefault() {
        return STALACTITE;
    }


}
