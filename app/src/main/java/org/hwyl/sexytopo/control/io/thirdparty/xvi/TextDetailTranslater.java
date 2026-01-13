package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextDetailTranslater extends SketchDetailTranslater<TextDetail> {
    private static final float CHAR_HEIGHT = 4.0f;
    private static final float INTER_CHAR_SPACE = 1f;
    private static final float LINE_SPACING = 1.2f;

    // Bit of a fudge factor to get text scale to be similar to likely text scale
    // More art than science ;)
    private static final float SCALE_FACTOR = 0.15f;

    /* ********** Glyph data copied from TopoDroid - thanks, Marco! ********** */
    // Each character maps to an array of line segment coordinates [x1, y1, x2, y2, ...]
    // Coordinates are in a 4-unit high space
    private static final Map<Character, int[]> GLYPH_DATA = new HashMap<>();

    static {
        // Special characters
        GLYPH_DATA.put(' ', new int[] {});  // Space (empty)
        GLYPH_DATA.put('_', new int[] {0, 0, 2, 0});
        GLYPH_DATA.put('+', new int[] {1, 1, 1, 3, 0, 2, 2, 2});
        GLYPH_DATA.put('-', new int[] {0, 2, 2, 2});
        GLYPH_DATA.put('?', new int[] {1, 0, 1, 2, 0, 3, 0, 4, 0, 4, 2, 4, 1, 2, 2, 3, 2, 3, 2, 4});
        GLYPH_DATA.put('/', new int[] {0, 0, 2, 4});
        GLYPH_DATA.put('<', new int[] {0, 2, 2, 3, 0, 2, 2, 1});
        GLYPH_DATA.put('>', new int[] {0, 3, 2, 2, 0, 1, 2, 2});

        // Letters A-Z
        GLYPH_DATA.put('A', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 2, 4, 2, 0, 0, 2, 2, 2});
        GLYPH_DATA.put('B', new int[] {0, 0, 0, 4, 0, 4, 2, 3, 0, 2, 2, 2, 0, 0, 2, 0, 2, 3, 2, 0});
        GLYPH_DATA.put('C', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 2, 0});
        GLYPH_DATA.put('D', new int[] {0, 0, 0, 4, 0, 4, 2, 3, 2, 3, 2, 0, 0, 0, 2, 0});
        GLYPH_DATA.put('E', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0});
        GLYPH_DATA.put('F', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2});
        GLYPH_DATA.put('G', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 2, 0, 2, 0, 2, 2, 1, 2, 2, 2});
        GLYPH_DATA.put('H', new int[] {0, 0, 0, 4, 0, 2, 2, 2, 2, 0, 2, 4});
        GLYPH_DATA.put('I', new int[] {0, 0, 0, 4});
        GLYPH_DATA.put('J', new int[] {0, 0, 1, 0, 1, 0, 1, 4, 0, 4, 2, 4});
        GLYPH_DATA.put('K', new int[] {0, 0, 0, 4, 0, 2, 2, 3, 0, 2, 2, 0});
        GLYPH_DATA.put('L', new int[] {0, 0, 0, 4, 0, 0, 2, 0});
        GLYPH_DATA.put('M', new int[] {0, 0, 0, 4, 0, 4, 1, 2, 1, 2, 2, 4, 2, 4, 2, 0});
        GLYPH_DATA.put('N', new int[] {0, 0, 0, 4, 0, 4, 2, 0, 2, 4, 2, 0});
        GLYPH_DATA.put('O', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 2, 0, 2, 4, 2, 0});
        GLYPH_DATA.put('P', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2, 2, 4, 2, 2});
        GLYPH_DATA.put('Q', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 1, 0, 1, 0, 2, 1, 2, 1, 2, 4, 1, 1, 2, 0});
        GLYPH_DATA.put('R', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 2, 4, 2, 2, 0, 2, 2, 2, 0, 2, 2, 0});
        GLYPH_DATA.put('S', new int[] {0, 2, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 0, 2, 2});
        GLYPH_DATA.put('T', new int[] {1, 0, 1, 4, 0, 4, 2, 4});
        GLYPH_DATA.put('U', new int[] {0, 0, 0, 4, 0, 0, 2, 0, 2, 4, 2, 0});
        GLYPH_DATA.put('V', new int[] {0, 4, 1, 0, 1, 0, 2, 4});
        GLYPH_DATA.put('W', new int[] {0, 0, 0, 4, 0, 0, 1, 2, 1, 2, 2, 0, 2, 0, 2, 4});
        GLYPH_DATA.put('X', new int[] {0, 0, 2, 4, 0, 4, 2, 0});
        GLYPH_DATA.put('Y', new int[] {0, 4, 1, 2, 1, 0, 1, 2, 1, 2, 2, 4});
        GLYPH_DATA.put('Z', new int[] {0, 4, 2, 4, 0, 0, 2, 4, 0, 0, 2, 0});

        // Numbers 0-9
        GLYPH_DATA.put('0', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 2, 0, 2, 4, 2, 0});
        GLYPH_DATA.put('1', new int[] {1, 0, 1, 4, 1, 3, 1, 4});
        GLYPH_DATA.put('2', new int[] {2, 4, 2, 3, 0, 0, 0, 1, 0, 4, 2, 4, 0, 1, 2, 3, 0, 0, 2, 0});
        GLYPH_DATA.put('3', new int[] {0, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 4, 2, 0});
        GLYPH_DATA.put('4', new int[] {1, 4, 0, 1, 1, 0, 1, 4, 0, 1, 2, 1});
        GLYPH_DATA.put('5', new int[] {0, 2, 1, 4, 1, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 0, 2, 2});
        GLYPH_DATA.put('6', new int[] {0, 0, 0, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 0, 2, 2});
        GLYPH_DATA.put('7', new int[] {0, 4, 2, 4, 0, 0, 2, 4});
        GLYPH_DATA.put('8', new int[] {0, 0, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 4, 2, 0});
        GLYPH_DATA.put('9', new int[] {0, 2, 2, 2, 0, 4, 2, 4, 0, 2, 0, 4, 2, 4, 2, 0});

        // Default for unsupported characters
        GLYPH_DATA.put('\0', new int[] {2, 2, 1, 4, 1, 4, 0, 2, 0, 2, 1, 0, 1, 0, 2, 2});  // Question mark box
    }

    private static List<PathDetail> getPathsForChar(char ch, Colour colour) {
        // Get glyph data, defaulting to the unknown character glyph
        int[] glyph = GLYPH_DATA.containsKey(ch) ? GLYPH_DATA.get(ch) : GLYPH_DATA.get('\0');

        List<PathDetail> glyphPaths = new ArrayList<>();
        for (int i = 0; i < glyph.length; i += 4) {
            Coord2D from = new Coord2D(glyph[i], -glyph[i + 1] + CHAR_HEIGHT);
            Coord2D to = new Coord2D(glyph[i + 2], -glyph[i + 3] + CHAR_HEIGHT);
            PathDetail pathDetail = new PathDetail(List.of(from, to), colour);
            glyphPaths.add(pathDetail);
        }
        return glyphPaths;
    }

    @Override
    public List<PathDetail> asPathDetails(TextDetail textDetail) {
        return getPathDetailsForText(textDetail);
    }

    static List<PathDetail> getPathDetailsForText(TextDetail textDetail) {
        List<PathDetail> pathDetails = new ArrayList<>();

        Colour colour = textDetail.getColour();
        String text = textDetail.getText();
        Coord2D position = textDetail.getPosition();
        float size = textDetail.getSize();
        float scale = size * SCALE_FACTOR;

        // Split text by newlines to handle multi-line text
        String[] lines = text.split("\n", -1);
        float lineHeight = CHAR_HEIGHT * LINE_SPACING * scale;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            String cleaned = line.toUpperCase();
            char[] chars = cleaned.toCharArray();

            // Calculate Y offset for this line
            float yOffset = lineIndex * lineHeight;
            Coord2D linePosition = position.plus(new Coord2D(0, yOffset));

            float xPos = 0;
            for (int i = 0; i < line.length(); i++) {
                char ch = chars[i];
                List<PathDetail> charPaths = getPathsForChar(ch, colour);

                // Letters such as "1" are vertical lines so have no width!
                // Set a minimum width to handle this
                float minWidth = INTER_CHAR_SPACE * scale;

                float charWidth = minWidth;
                for (PathDetail pathDetail : charPaths) {
                    PathDetail scaledPath = pathDetail.scale(scale);
                    // adjust for position in string
                    PathDetail translatedPath = scaledPath.translate(new Coord2D(xPos, 0));
                    // adjust for position on sketch
                    translatedPath = translatedPath.translate(linePosition);
                    pathDetails.add(translatedPath);

                    // keep a total of widest bit to get total char width
                    charWidth = Math.max(charWidth, scaledPath.getWidth());
                }

                float interCharSpace = INTER_CHAR_SPACE * scale;
                xPos += charWidth + interCharSpace;
            }
        }

        return pathDetails;
    }


}
