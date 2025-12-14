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
    TEXT(R.string.symbol_text, R.drawable.text, "text.svg", false, "label", null),

    // ********** Cave geometry **********
    ENTRANCE(R.string.symbol_entrance, R.drawable.symbol_uis_entrance, "symbol_uis_entrance.svg", true, "entrance",
        new float[][] {{15, 29, 20, 9, 20, 9, 25, 29, 25, 29, 15, 29}}),
    GRADIENT(R.string.symbol_gradient, R.drawable.symbol_uis_gradient, "symbol_uis_gradient.svg", true, "gradient",
        new float[][] {{10, 10, 30, 30}, {30, 30, 25, 28}, {30, 30, 28, 25}}),
    TOO_TIGHT(R.string.symbol_too_tight, R.drawable.symbol_uis_too_tight, "symbol_uis_too_tight.svg", true, "narrow-end",
        new float[][] {{15, 7.33747f, 14.58205f, 32.66253f}, {25.29411f, 7, 25, 33.00305f}}),

    // ********** Floor stuff **********
    SAND(R.string.symbol_sand, R.drawable.symbol_uis_sand, "symbol_uis_sand.svg", false, "sand",
        // Approximate circles with crosses
        new float[][] {
            {5, 5, 7, 5}, {6, 4, 6, 6},  // Cross at (6,5)
            {13, 9, 15, 9}, {14, 8, 14, 10},  // Cross at (14,9)
            {22, 21, 24, 21}, {23, 20, 23, 22},  // Cross at (23,21)
            {32, 13, 34, 13}, {33, 12, 33, 14},  // Cross at (33,13)
            {34, 3, 36, 3}, {35, 2, 35, 4},  // Cross at (35,3)
            {6, 18, 8, 18}, {7, 17, 7, 19},  // Cross at (7,18)
            {9, 30, 11, 30}, {10, 29, 10, 31},  // Cross at (10,30)
            {26, 35, 28, 35}, {27, 34, 27, 36},  // Cross at (27,35)
            {25, 2, 27, 2}, {26, 1, 26, 3},  // Cross at (26,2)
            {33, 27, 35, 27}, {34, 26, 34, 28}  // Cross at (34,27)
        }),
    CLAY(R.string.symbol_clay, R.drawable.symbol_uis_clay, "symbol_uis_clay.svg", false, "clay",
        new float[][] {
            {5, 7, 9, 7}, {18, 6, 23, 6}, {31, 11, 36, 10},
            {7, 20, 12, 19}, {20, 23, 25, 23}, {32, 21, 37, 21},
            {18, 33, 22, 32}, {4, 33, 8, 32}, {34, 33, 38, 32}
        }),
    PEBBLES(R.string.symbol_pebbles, R.drawable.symbol_uis_pebbles, "symbol_uis_pebbles.svg", false, "pebbles",
        new float[][] {
            // Approximate ovoids with rectangles (wider than tall)
            {19, 20, 27, 20, 27, 25, 19, 25, 19, 20},  // 8×5
            {30, 14, 38, 14, 38, 19, 30, 19, 30, 14},  // 8×5
            {9, 16, 17, 16, 17, 21, 9, 21, 9, 16}      // 8×5
        }),
    BLOCKS(R.string.symbol_blocks, R.drawable.symbol_uis_blocks, "symbol_uis_blocks.svg", false, "blocks",
        new float[][] {
            {8.84876f, 9.42738f, 27.58284f, 10.27319f, 12.06031f, 23.80615f, 8.84876f, 9.42738f},
            {26.69074f, 17.88548f, 31.15124f, 19.57709f, 27.58284f, 30.57262f, 13.30926f, 30.57262f, 26.69074f, 17.88548f},
            {8.84876f, 9.42738f, 13.30926f, 13.65643f, 12.06031f, 23.80615f},
            {13.30926f, 13.65643f, 27.58284f, 10.27319f},
            {26.69074f, 17.88548f, 27.58284f, 20.42291f, 25.49501f, 23.23703f, 13.30926f, 30.57262f},
            {27.06688f, 30.19221f, 25.49567f, 23.26106f},
            {31.15124f, 19.57709f, 27.58284f, 20.42291f}
        }),

    // ********** Speleothems **********
    STALACTITE(R.string.symbol_stalactite, R.drawable.symbol_uis_stalactite, "symbol_uis_stalactite.svg", false, "stalactite",
        new float[][] {{20, 35, 20, 12.5f, 20, 12.5f, 10, 5}, {20, 12.5f, 30, 5}}),
    STALAGMITE(R.string.symbol_stalagmite, R.drawable.symbol_uis_stalagmite, "symbol_uis_stalagmite.svg", false, "stalagmite",
        new float[][] {{20, 5, 20, 27.5f, 20, 27.5f, 10, 35}, {20, 27.5f, 30, 35}}),
    COLUMN(R.string.symbol_column, R.drawable.symbol_uis_column, "symbol_uis_column.svg", false, "pillar",
        new float[][] {
            {20, 26, 20, 14},  // Vertical middle
            {10, 4, 20, 14, 20, 14, 30, 4},  // Top V
            {10, 36, 20, 26, 20, 26, 30, 36}  // Bottom inverted V
        }),
    CURTAIN(R.string.symbol_curtain, R.drawable.symbol_uis_curtain, "symbol_uis_curtain.svg", false, "curtain",
        new float[][] {
            {10.12515f, 5, 20, 15.94756f, 20, 15.94756f, 29.87485f, 5},  // Top wavy
            {19.95307f, 15.89207f, 19.95307f, 20.89207f, 19.95307f, 20.89207f, 13.19741f, 25.89207f, 13.19741f, 25.89207f, 19.95307f, 30.89207f, 19.95307f, 30.89207f, 19.95307f, 35.89207f}  // Bottom wavy
        }),
    STRAWS(R.string.symbol_straws, R.drawable.symbol_uis_straws, "symbol_uis_straws.svg", false, "soda-straw",
        new float[][] {
            {5, 12.5f, 35, 12.5f},  // Horizontal line
            {10, 12.5f, 10, 20},  // Straw 1
            {20, 12.5f, 20, 27.5f},  // Straw 2
            {25, 12.5f, 25, 22.5f},  // Straw 3
            {30, 12.5f, 30, 25.5f},  // Straw 4
            {32.5f, 12.5f, 32.5f, 25}  // Straw 5
        }),
    HELICTITES(R.string.symbol_helictites, R.drawable.symbol_uis_helictite, "symbol_uis_helictite.svg", false, "helictite",
        new float[][] {
            {20, 8, 20, 32},  // Vertical line
            {12, 8, 12, 20, 12, 20, 28, 20, 28, 20, 28, 32}  // Horizontal cross
        }),
    CRYSTALS(R.string.symbol_crystals, R.drawable.symbol_uis_crystal, "symbol_uis_crystal.svg", false, "crystal",
        new float[][] {
            {7.31238f, 19.91065f, 32.68762f, 20.08935f},  // Horizontal
            {10, 7, 30, 33},  // Diagonal 1 (approx -55 degrees)
            {10, 33, 30, 7}  // Diagonal 2 (approx +55 degrees)
        }),
    GOUR(R.string.symbol_gour, R.drawable.symbol_uis_gour, "symbol_uis_gour.svg", true, "rimstone-dam",
        new float[][] {
            // Simplified arc - 3 lines
            {5, 26.5f, 12, 20, 20, 17.5f, 28, 20, 35, 26.5f}
        }),

    // ********** Fluids **********
    WATER_FLOW(R.string.symbol_water_flow, R.drawable.symbol_uis_flow, "symbol_uis_flow.svg", true, "water-flow",
        new float[][] {
            // Wavy line - stops at arrow head
            {19.64792f, 34.47846f, 21, 30, 22, 26, 22, 24, 20, 20, 18, 16, 18, 12, 19.75f, 9.77f},
            // Arrow head
            {16.62578f, 9.7767f, 19.75460f, 4.99145f, 19.75460f, 4.99145f, 22.88342f, 9.7767f, 22.88342f, 9.7767f, 16.62578f, 9.7767f}
        }),
    AIR_DRAUGHT(R.string.symbol_air_draught, R.drawable.symbol_uis_air_draught, "symbol_uis_air_draught.svg", true, "air-draught",
        new float[][] {
            {19.75f, 9.77f, 19.71098f, 29.7324f},  // Main vertical line - starts at arrow base
            // Arrow head
            {16.62578f, 9.7767f, 19.75460f, 4.99145f, 19.75460f, 4.99145f, 22.88342f, 9.7767f, 22.88342f, 9.7767f, 16.62578f, 9.7767f},
            // Side wavy lines
            {19.6282f, 29.44624f, 24.05512f, 35.14666f},
            {19.81955f, 24.89208f, 24.24647f, 30.59251f}
        }),

    // ********** Other **********
    GUANO(R.string.symbol_guano, R.drawable.symbol_uis_guano, "symbol_uis_guano.svg", false, "guano",
        new float[][] {
            // M shape: shorter ends, longer middle
            {8, 25, 11, 19, 11, 19, 20, 35, 29, 19, 29, 19, 32, 25}
        }),
    DEBRIS(R.string.symbol_debris, R.drawable.symbol_uis_debris, "symbol_uis_debris.svg", false, "debris",
        new float[][] {
            {14, 12, 20, 15.6f, 20, 15.6f, 12, 18, 12, 18, 14, 12},  // Triangle 1
            {24, 12, 28, 18, 28, 18, 22, 18, 22, 18, 24, 12},  // Triangle 2
            {18, 20, 24, 26, 24, 26, 16, 26, 16, 26, 18, 20}  // Triangle 3
        });

    private static Resources resources;

    private final int viewId;
    private final int stringId;
    private final int drawableId;
    private final String svgFilename;
    private String rawSvg;

    private final boolean isDirectional;

    private final String therionName;

    // XVI export path data: array of paths, each path is array of coordinates [x1,y1,x2,y2,...]
    // Coordinates are in 40x40 viewBox. Null means use SVG parsing fallback.
    private final float[][] xviPathData;


    private static final Symbol DEFAULT = TEXT;

    public static final String SVG_DIR = "svg/symbols/";

    Symbol(int stringId, int drawableId, String svgFilename, boolean isDirectional, String therionName, float[][] xviPathData) {
        this.stringId = stringId;
        this.drawableId = drawableId;
        this.svgFilename = svgFilename;
        this.isDirectional = isDirectional;
        this.therionName = therionName;
        this.xviPathData = xviPathData;
        this.viewId = android.view.View.generateViewId();
    }

    public static void setResources(Resources resources) {
        Symbol.resources = resources;
    }

    public static Symbol fromString(String name) {
        return name == null? DEFAULT : Symbol.valueOf(name);
    }


    public int getButtonViewId() {
        return viewId;
    }

    public String getSvgRefId() {
        String underscored = this.toString().toLowerCase().replaceAll(" ", "_");
        return "symbol_" + underscored;
    }

    public String getName() {
        return resources == null? this.toString() : resources.getString(stringId);
    }

    public boolean isDirectional() {
        return isDirectional;
    }

    public String getTherionName() {
        return therionName;
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

    /**
     * Get XVI path data for this symbol.
     * Returns array of paths, where each path is [x1, y1, x2, y2, ...]
     * Coordinates are in 40x40 viewBox.
     * Returns null if no XVI path data defined (will fall back to SVG parsing).
     */
    public float[][] getXviPathData() {
        return xviPathData;
    }


}
