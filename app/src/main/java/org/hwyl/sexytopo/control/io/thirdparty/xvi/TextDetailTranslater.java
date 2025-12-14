package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;

import java.util.ArrayList;
import java.util.List;

public class TextDetailTranslater extends SketchDetailTranslater<TextDetail> {
    private static final float CHAR_HEIGHT = 4.0f;
    private static final float INTER_CHAR_SPACE = 1f;
    private static final float LINE_SPACING = 1.2f;

    // Bit of a fudge factor to get text scale to be similar to likely text scale
    // More art than science ;)
    private static final float SCALE_FACTOR = 0.15f;


    /* ********** copied from TopoDroid - thanks, Marco! ********** */
    static final private int[] CHAR_ANY = {2, 2, 1, 4, 1, 4, 0, 2, 0, 2, 1, 0, 1, 0, 2, 2};
    static final private int[] CHAR_SPACE = {};
    static final private int[] CHAR_UNDERSCORE = {0, 0, 2, 0};
    static final private int[] CHAR_PLUS = {1, 1, 1, 3, 0, 2, 2, 2};
    static final private int[] CHAR_MINUS = {0, 2, 2, 2};
    static final private int[] CHAR_QUESTION = {1, 0, 1, 2, 0, 3, 0, 4, 0, 4, 2, 4, 1, 2, 2, 3, 2, 3, 2, 4};
    static final private int[] CHAR_SLASH = {0, 0, 2, 4};
    static final private int[] CHAR_LESS = {0, 2, 2, 3, 0, 2, 2, 1};
    static final private int[] CHAR_MORE = {0, 3, 2, 2, 0, 1, 2, 2};

    /*static final CHAR_TO_GLYPH = {
    }*/

    static final private int[][] GLYPH_AZ = {
      {0, 0, 0, 4, 0, 4, 2, 4, 2, 4, 2, 0, 0, 2, 2, 2}, // A
      {0, 0, 0, 4, 0, 4, 2, 3, 0, 2, 2, 2, 0, 0, 2, 0, 2, 3, 2, 0}, 
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 2, 0}, 
      {0, 0, 0, 4, 0, 4, 2, 3, 2, 3, 2, 0, 0, 0, 2, 0}, 
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0}, // E
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2}, 
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 2, 0, 2, 0, 2, 2, 1, 2, 2, 2}, // G
      {0, 0, 0, 4, 0, 2, 2, 2, 2, 0, 2, 4}, 
      {0, 0, 0, 4}, 
      {0, 0, 1, 0, 1, 0, 1, 4, 0, 4, 2, 4}, // J
      {0, 0, 0, 4, 0, 2, 2, 3, 0, 2, 2, 0}, 
      {0, 0, 0, 4, 0, 0, 2, 0}, 
      {0, 0, 0, 4, 0, 4, 1, 2, 1, 2, 2, 4, 2, 4, 2, 0}, 
      {0, 0, 0, 4, 0, 4, 2, 0, 2, 4, 2, 0}, 
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 2, 0, 2, 4, 2, 0}, // O
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2, 2, 4, 2, 2, }, 
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 1, 0, 1, 0, 2, 1, 2, 1, 2, 4, 1, 1, 2, 0}, 
      {0, 0, 0, 4, 0, 4, 2, 4, 2, 4, 2, 2, 0, 2, 2, 2, 0, 2, 2, 0}, 
      {0, 2, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 0, 2, 2}, // S
      {1, 0, 1, 4, 0, 4, 2, 4}, 
      {0, 0, 0, 4, 0, 0, 2, 0, 2, 4, 2, 0}, 
      {0, 4, 1, 0, 1, 0, 2, 4}, 
      {0, 0, 0, 4, 0, 0, 1, 2, 1, 2, 2, 0, 2, 0, 2, 4}, // W
      {0, 0, 2, 4, 0, 4, 2, 0}, 
      {0, 4, 1, 2, 1, 0, 1, 2, 1, 2, 2, 4}, 
      {0, 4, 2, 4, 0, 0, 2, 4, 0, 0, 2, 0}
    };
    static final private int[][] GLYPH_09 = {
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 0, 2, 0, 2, 4, 2, 0}, // 0
      {1, 0, 1, 4, 1, 3, 1, 4}, 
      {2, 4, 2, 3, 0, 0, 0, 1, 0, 4, 2, 4, 0, 1, 2, 3, 0, 0, 2, 0}, // 2
      {0, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 4, 2, 0}, 
      {1, 4, 0, 1, 1, 0, 1, 4, 0, 1, 2, 1}, 
      {0, 2, 1, 4, 1, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 0, 2, 2}, // 5
      {0, 0, 0, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 0, 2, 2}, 
      {0, 4, 2, 4, 0, 0, 2, 4}, 
      {0, 0, 0, 4, 0, 4, 2, 4, 0, 2, 2, 2, 0, 0, 2, 0, 2, 4, 2, 0}, 
      {0, 2, 2, 2, 0, 4, 2, 4, 0, 2, 0, 4, 2, 4, 2, 0} // 9
   };

    /**
     * Get the glyph array for a character.
     * @param ch Character to get glyph for
     * @return Coordinate array for the glyph, or null if character not supported
     */
    private static int[] getGlyphCode(char ch) {
        if (ch >= 'A' && ch <= 'Z') {
            return GLYPH_AZ[ch - 'A'];
        } else if (ch >= '0' && ch <= '9') {
            return GLYPH_09[ch - '0'];
        } else if (ch == ' ') {
            return CHAR_SPACE;
        } else if (ch == '_') {
            return CHAR_UNDERSCORE;
        } else if (ch == '+') {
            return CHAR_PLUS;
        } else if (ch == '-') {
            return CHAR_MINUS;
        } else if (ch == '?') {
            return CHAR_QUESTION;
        } else if (ch == '/') {
            return CHAR_SLASH;
        } else if (ch == '<') {
            return CHAR_LESS;
        } else if (ch == '>') {
            return CHAR_MORE;
        } else {
            return CHAR_ANY;
        }
    }

    private static List<PathDetail> getPathsForChar(char ch, Colour colour) {
        int[] glyph = getGlyphCode(ch);
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
