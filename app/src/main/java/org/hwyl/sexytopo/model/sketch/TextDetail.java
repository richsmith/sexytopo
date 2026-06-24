package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;

public class TextDetail extends SinglePositionDetail implements AutoScalableDetail {

    private final String text;
    private final float size;

    public TextDetail(Coord2D location, String text, Colour colour, float size) {
        super(colour, location);
        this.text = text;
        this.size = size;

        // Expand the bounding box (which the superclass seeds with just the position) to cover the
        // rendered glyphs. Text is drawn with the position as the first line's left edge and
        // baseline, so glyphs extend rightwards and upwards from it and extra lines extend
        // downwards. With a real box, the inherited visibility check and getDistanceFrom both work
        // without re-deriving the geometry.
        String[] lines = text.split("\n", -1);
        int longestLine = 0;
        for (String line : lines) {
            longestLine = Math.max(longestLine, line.length());
        }
        // Text size represents approximate height; estimate width as proportional to length.
        float width = longestLine * size * 0.6f;
        updateBoundingBox(new Coord2D(location.x + width, location.y - size));
        updateBoundingBox(new Coord2D(location.x, location.y + size * (lines.length - 1)));
    }

    public String getText() {
        return text;
    }

    public float getSize() {
        return size;
    }

    @Override
    public TextDetail translate(Coord2D translation) {
        return new TextDetail(getPosition().plus(translation), getText(), getColour(), getSize());
    }

    @Override
    public TextDetail scale(float scale) {
        return new TextDetail(getPosition(), getText(), getColour(), getSize() * scale);
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        // Within the text's bounds, return a scaled distance to the box centre. This gives text
        // priority without completely blocking deletion of a line drawn over it.
        if (point.x >= getLeft()
                && point.x <= getRight()
                && point.y >= getTop()
                && point.y <= getBottom()) {
            Coord2D centre =
                    new Coord2D((getLeft() + getRight()) / 2, (getTop() + getBottom()) / 2);
            return Space2DUtils.getDistance(point, centre) * 0.5f;
        }

        // Otherwise, return the distance to the nearest edge of the box.
        float distX = Math.max(0, Math.max(getLeft() - point.x, point.x - getRight()));
        float distY = Math.max(0, Math.max(getTop() - point.y, point.y - getBottom()));
        return (float) Math.sqrt(distX * distX + distY * distY);
    }
}
