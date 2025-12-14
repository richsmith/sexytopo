package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolDetailTranslater extends SketchDetailTranslater<SymbolDetail> {

    private static final float SVG_VIEWBOX_SIZE = 40.0f;

    // XVI path data for symbols
    // Each symbol maps to an array of paths, where each path is [x1, y1, x2, y2, ...]
    // Coordinates are in 40x40 viewBox
    private static final Map<Symbol, float[][]> XVI_PATH_DATA = new HashMap<>();

    static {
        // ********** Cave geometry **********
        XVI_PATH_DATA.put(Symbol.ENTRANCE, new float[][] {
            {15, 29, 20, 9, 20, 9, 25, 29, 25, 29, 15, 29}  // Triangle
        });

        XVI_PATH_DATA.put(Symbol.GRADIENT, new float[][] {
            {10, 10, 30, 30},  // Main diagonal line
            {30, 30, 25, 28},  // Arrow head left
            {30, 30, 28, 25}   // Arrow head up
        });

        XVI_PATH_DATA.put(Symbol.TOO_TIGHT, new float[][] {
            {15, 7.33747f, 14.58205f, 32.66253f},  // Left vertical line
            {25.29411f, 7, 25, 33.00305f}          // Right vertical line
        });

        // ********** Floor stuff **********
        XVI_PATH_DATA.put(Symbol.SAND, new float[][] {
            // Approximate circles with crosses
            {5, 5, 7, 5}, {6, 4, 6, 6},          // Cross at (6,5)
            {13, 9, 15, 9}, {14, 8, 14, 10},     // Cross at (14,9)
            {22, 21, 24, 21}, {23, 20, 23, 22},  // Cross at (23,21)
            {32, 13, 34, 13}, {33, 12, 33, 14},  // Cross at (33,13)
            {34, 3, 36, 3}, {35, 2, 35, 4},      // Cross at (35,3)
            {6, 18, 8, 18}, {7, 17, 7, 19},      // Cross at (7,18)
            {9, 30, 11, 30}, {10, 29, 10, 31},   // Cross at (10,30)
            {26, 35, 28, 35}, {27, 34, 27, 36},  // Cross at (27,35)
            {25, 2, 27, 2}, {26, 1, 26, 3},      // Cross at (26,2)
            {33, 27, 35, 27}, {34, 26, 34, 28}   // Cross at (34,27)
        });

        XVI_PATH_DATA.put(Symbol.CLAY, new float[][] {
            {5, 7, 9, 7}, {18, 6, 23, 6}, {31, 11, 36, 10},
            {7, 20, 12, 19}, {20, 23, 25, 23}, {32, 21, 37, 21},
            {18, 33, 22, 32}, {4, 33, 8, 32}, {34, 33, 38, 32}
        });

        XVI_PATH_DATA.put(Symbol.PEBBLES, new float[][] {
            // Approximate ovoids with rectangles (wider than tall)
            {19, 20, 27, 20, 27, 25, 19, 25, 19, 20},  // 8×5
            {30, 14, 38, 14, 38, 19, 30, 19, 30, 14},  // 8×5
            {9, 16, 17, 16, 17, 21, 9, 21, 9, 16}      // 8×5
        });

        XVI_PATH_DATA.put(Symbol.BLOCKS, new float[][] {
            {8.84876f, 9.42738f, 27.58284f, 10.27319f, 12.06031f, 23.80615f, 8.84876f, 9.42738f},
            {26.69074f, 17.88548f, 31.15124f, 19.57709f, 27.58284f, 30.57262f, 13.30926f, 30.57262f, 26.69074f, 17.88548f},
            {8.84876f, 9.42738f, 13.30926f, 13.65643f, 12.06031f, 23.80615f},
            {13.30926f, 13.65643f, 27.58284f, 10.27319f},
            {26.69074f, 17.88548f, 27.58284f, 20.42291f, 25.49501f, 23.23703f, 13.30926f, 30.57262f},
            {27.06688f, 30.19221f, 25.49567f, 23.26106f},
            {31.15124f, 19.57709f, 27.58284f, 20.42291f}
        });

        // ********** Speleothems **********
        XVI_PATH_DATA.put(Symbol.STALACTITE, new float[][] {
            {20, 35, 20, 12.5f, 20, 12.5f, 10, 5},  // Vertical + left diagonal
            {20, 12.5f, 30, 5}                      // Right diagonal
        });

        XVI_PATH_DATA.put(Symbol.STALAGMITE, new float[][] {
            {20, 5, 20, 27.5f, 20, 27.5f, 10, 35},  // Vertical + left diagonal
            {20, 27.5f, 30, 35}                     // Right diagonal
        });

        XVI_PATH_DATA.put(Symbol.COLUMN, new float[][] {
            {20, 26, 20, 14},                       // Vertical middle
            {10, 4, 20, 14, 20, 14, 30, 4},         // Top V
            {10, 36, 20, 26, 20, 26, 30, 36}        // Bottom inverted V
        });

        XVI_PATH_DATA.put(Symbol.CURTAIN, new float[][] {
            {10.12515f, 5, 20, 15.94756f, 20, 15.94756f, 29.87485f, 5},  // Top wavy
            {19.95307f, 15.89207f, 19.95307f, 20.89207f, 19.95307f, 20.89207f, 13.19741f, 25.89207f,
             13.19741f, 25.89207f, 19.95307f, 30.89207f, 19.95307f, 30.89207f, 19.95307f, 35.89207f}  // Bottom wavy
        });

        XVI_PATH_DATA.put(Symbol.STRAWS, new float[][] {
            {5, 12.5f, 35, 12.5f},     // Horizontal line
            {10, 12.5f, 10, 20},       // Straw 1
            {20, 12.5f, 20, 27.5f},    // Straw 2
            {25, 12.5f, 25, 22.5f},    // Straw 3
            {30, 12.5f, 30, 25.5f},    // Straw 4
            {32.5f, 12.5f, 32.5f, 25}  // Straw 5
        });

        XVI_PATH_DATA.put(Symbol.HELICTITES, new float[][] {
            {20, 8, 20, 32},                                 // Vertical line
            {12, 8, 12, 20, 12, 20, 28, 20, 28, 20, 28, 32}  // Horizontal cross
        });

        XVI_PATH_DATA.put(Symbol.CRYSTALS, new float[][] {
            {7.31238f, 19.91065f, 32.68762f, 20.08935f},  // Horizontal
            {10, 7, 30, 33},                               // Diagonal 1 (approx -55 degrees)
            {10, 33, 30, 7}                                // Diagonal 2 (approx +55 degrees)
        });

        XVI_PATH_DATA.put(Symbol.GOUR, new float[][] {
            // Simplified arc - 3 lines
            {5, 26.5f, 12, 20, 20, 17.5f, 28, 20, 35, 26.5f}
        });

        // ********** Fluids **********
        XVI_PATH_DATA.put(Symbol.WATER_FLOW, new float[][] {
            // Wavy line - stops at arrow head
            {19.64792f, 34.47846f, 21, 30, 22, 26, 22, 24, 20, 20, 18, 16, 18, 12, 19.75f, 9.77f},
            // Arrow head
            {16.62578f, 9.7767f, 19.75460f, 4.99145f, 19.75460f, 4.99145f, 22.88342f, 9.7767f, 22.88342f, 9.7767f, 16.62578f, 9.7767f}
        });

        XVI_PATH_DATA.put(Symbol.AIR_DRAUGHT, new float[][] {
            {19.75f, 9.77f, 19.71098f, 29.7324f},  // Main vertical line - starts at arrow base
            // Arrow head
            {16.62578f, 9.7767f, 19.75460f, 4.99145f, 19.75460f, 4.99145f, 22.88342f, 9.7767f, 22.88342f, 9.7767f, 16.62578f, 9.7767f},
            // Side wavy lines
            {19.6282f, 29.44624f, 24.05512f, 35.14666f},
            {19.81955f, 24.89208f, 24.24647f, 30.59251f}
        });

        // ********** Other **********
        XVI_PATH_DATA.put(Symbol.GUANO, new float[][] {
            // M shape: shorter ends, longer middle
            {8, 25, 11, 19, 11, 19, 20, 35, 29, 19, 29, 19, 32, 25}
        });

        XVI_PATH_DATA.put(Symbol.DEBRIS, new float[][] {
            {14, 12, 20, 15.6f, 20, 15.6f, 12, 18, 12, 18, 14, 12},  // Triangle 1
            {24, 12, 28, 18, 28, 18, 22, 18, 22, 18, 24, 12},        // Triangle 2
            {18, 20, 24, 26, 24, 26, 16, 26, 16, 26, 18, 20}         // Triangle 3
        });
    }

    @Override
    public List<PathDetail> asPathDetails(SymbolDetail symbolDetail) {
        List<PathDetail> pathDetails = new ArrayList<>();

        float[][] pathData = XVI_PATH_DATA.get(symbolDetail.getSymbol());
        if (pathData == null) {
            return pathDetails; // Return empty list if no path data defined
        }

        float scale = symbolDetail.getSize() / SVG_VIEWBOX_SIZE;
        float centerOffset = SVG_VIEWBOX_SIZE / 2.0f;

        for (float[] path : pathData) {
            List<Coord2D> coords = new ArrayList<>();

            // Convert coordinate pairs to Coord2D
            for (int i = 0; i < path.length; i += 2) {
                float x = path[i];
                float y = path[i + 1];

                // Center the coordinate (move origin to center of viewBox)
                x -= centerOffset;
                y -= centerOffset;

                // Scale
                x *= scale;
                y *= scale;

                // Rotate
                if (symbolDetail.getAngle() != 0) {
                    double angleRadians = Math.toRadians(symbolDetail.getAngle());
                    double cos = Math.cos(angleRadians);
                    double sin = Math.sin(angleRadians);
                    float rotatedX = (float) (x * cos - y * sin);
                    float rotatedY = (float) (x * sin + y * cos);
                    x = rotatedX;
                    y = rotatedY;
                }

                // Translate to symbol position
                x += symbolDetail.getPosition().x;
                y += symbolDetail.getPosition().y;

                coords.add(new Coord2D(x, y));
            }

            if (coords.size() >= 2) {
                PathDetail pathDetail = new PathDetail(coords, symbolDetail.getColour());
                pathDetails.add(pathDetail);
            }
        }

        return pathDetails;
    }
}
